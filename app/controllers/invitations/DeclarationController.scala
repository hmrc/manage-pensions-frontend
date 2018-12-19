/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.invitations

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.{InvitationConnector, InvitationsCacheConnector, SchemeDetailsConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.DeclarationFormProvider
import identifiers.SchemeSrnId
import identifiers.invitations._
import models._
import models.requests.DataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Navigator
import utils.annotations.AcceptInvitation
import views.html.invitations.declaration

import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
                                       formProvider: DeclarationFormProvider,
                                       auth: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       schemeDetailsConnector: SchemeDetailsConnector,
                                       invitationsCacheConnector: InvitationsCacheConnector,
                                       invitationConnector: InvitationConnector,
                                       @AcceptInvitation navigator: Navigator
                                     )(implicit val ec: ExecutionContext) extends FrontendController with I18nSupport with Retrievals {
  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>
      (DoYouHaveWorkingKnowledgeId and SchemeSrnId).retrieve.right.map {
        case haveWorkingKnowledge ~ srn =>
          for {
            details <- schemeDetailsConnector.getSchemeDetails(request.psaId.id, "srn", srn)
            _ <- userAnswersCacheConnector.save(SchemeNameId, details.schemeDetails.name)
            _ <- userAnswersCacheConnector.save(IsMasterTrustId, details.schemeDetails.isMasterTrust)
            _ <- userAnswersCacheConnector.save(PSTRId, details.schemeDetails.pstr.getOrElse(""))
          } yield {
            Ok(declaration(appConfig, haveWorkingKnowledge, details.schemeDetails.isMasterTrust, form))
          }
      }
  }

  def onSubmit(): Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          (DoYouHaveWorkingKnowledgeId and IsMasterTrustId).retrieve.right.map {
            case haveWorkingKnowledge ~ isMasterTrust =>
              Future.successful(BadRequest(declaration(appConfig, haveWorkingKnowledge, isMasterTrust, formWithErrors)))
          },
        declaration => {
          (PSTRId and DoYouHaveWorkingKnowledgeId).retrieve.right.map {
            case pstr ~ haveWorkingKnowledge =>
              acceptInviteAndRedirect(pstr, haveWorkingKnowledge, declaration)
          }
        }
      )
  }

  private def acceptInviteAndRedirect(pstr: String, haveWorkingKnowledge: Boolean, declaration: Boolean)
                                     (implicit request: DataRequest[AnyContent]): Future[Result] = {
    val userAnswers = request.userAnswers

    invitationsCacheConnector.get(pstr, request.psaId).flatMap { invitations =>
      invitations.headOption match {
        case Some(invitation) =>
          val acceptedInvitation = AcceptedInvitation(invitation.pstr, request.psaId, invitation.inviterPsaId, declaration,
            haveWorkingKnowledge, userAnswers.json.validate[PensionAdviserDetails](PensionAdviserDetails.userAnswerReads).asOpt)

          invitationConnector.acceptInvite(acceptedInvitation).flatMap { _ =>
            invitationsCacheConnector.remove(invitation.pstr, request.psaId).map { _ =>
              Redirect(navigator.nextPage(DeclarationId, NormalMode, userAnswers))
            }
          }
        case _ =>
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
    }
  }
}
