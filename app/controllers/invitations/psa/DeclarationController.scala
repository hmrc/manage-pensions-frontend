/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.invitations.psa

import com.google.inject.Inject
import connectors.scheme.SchemeDetailsConnector
import connectors.{InvitationConnector, InvitationsCacheConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.psa.DeclarationFormProvider
import identifiers.invitations._
import identifiers.{SchemeSrnId, SchemeTypeId, SchemeNameId => GetSchemeNameId}
import models.SchemeType.MasterTrust
import models._
import models.requests.DataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Navigator
import utils.annotations.AcceptInvitation
import views.html.invitations.psa.declaration

import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       formProvider: DeclarationFormProvider,
                                       auth: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       schemeDetailsConnector: SchemeDetailsConnector,
                                       invitationsCacheConnector: InvitationsCacheConnector,
                                       invitationConnector: InvitationConnector,
                                       @AcceptInvitation navigator: Navigator,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: declaration
                                     )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (auth() andThen getData andThen requireData).async {
    implicit request =>
      (DoYouHaveWorkingKnowledgeId and SchemeSrnId).retrieve.map {
        case haveWorkingKnowledge ~ srn =>
          schemeDetailsConnector.getSchemeDetails(
            psaId = request.psaIdOrException.id,
            idNumber = srn,
            schemeIdType = "srn"
          ) flatMap { details =>
            (details.get(GetSchemeNameId), details.get(SchemeTypeId), details.get(IsRacDacId)) match {
              case (Some(name), someSchemeType, isRacDac) =>

                val isMasterTrust = (someSchemeType, isRacDac) match {
                  case (Some(schemeType), None | Some(false)) => schemeType.equals(MasterTrust)
                  case (_, Some(true)) => false
                  case _ => throw new IllegalArgumentException("Scheme Type missing for Non RacDac scheme")
                }

                for {
                  _ <- userAnswersCacheConnector.save(SchemeNameId, name)
                  _ <- userAnswersCacheConnector.save(IsMasterTrustId, isMasterTrust)
                  _ <- userAnswersCacheConnector.save(PSTRId, details.get(PSTRId).getOrElse(""))
                } yield {
                  Ok(view(haveWorkingKnowledge, isMasterTrust, form))
                }
              case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))

            }
          }

      }
  }

  def onSubmit(): Action[AnyContent] = (auth() andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          (DoYouHaveWorkingKnowledgeId and IsMasterTrustId).retrieve.map {
            case haveWorkingKnowledge ~ isMasterTrust =>
              Future.successful(BadRequest(view(haveWorkingKnowledge, isMasterTrust, formWithErrors)))
          },
        declaration => {
          (PSTRId and DoYouHaveWorkingKnowledgeId).retrieve.map {
            case pstr ~ haveWorkingKnowledge =>
              acceptInviteAndRedirect(pstr, haveWorkingKnowledge, declaration)
          }
        }
      )
  }

  private def acceptInviteAndRedirect(pstr: String, haveWorkingKnowledge: Boolean, declaration: Boolean)
                                     (implicit request: DataRequest[AnyContent]): Future[Result] = {
    val userAnswers = request.userAnswers

    invitationsCacheConnector.get(pstr, request.psaIdOrException).flatMap { invitations =>
      invitations.headOption match {
        case Some(invitation) =>
          val acceptedInvitation =
            AcceptedInvitation(
              pstr = invitation.pstr,
              inviteePsaId = request.psaIdOrException,
              inviterPsaId = invitation.inviterPsaId,
              declaration = declaration,
              declarationDuties = haveWorkingKnowledge,
              pensionAdviserDetails =
                userAnswers
                  .json
                  .validate[PensionAdviserDetails](PensionAdviserDetails.userAnswerReads)
                  .asOpt
            )

          invitationConnector.acceptInvite(acceptedInvitation).flatMap { _ =>
            invitationsCacheConnector.remove(invitation.pstr, request.psaIdOrException).map { _ =>
              Redirect(navigator.nextPage(DeclarationId, NormalMode, userAnswers))
            }
          }
        case _ =>
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
      }
    }
  }
}
