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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Navigator
import utils.annotations.AcceptInvitation
import views.html.invitations.declaration

import scala.concurrent.Future

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
                                     ) extends FrontendController with I18nSupport with Retrievals {
  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>
      (HaveYouEmployedPensionAdviserId and SchemeSrnId).retrieve.right.map {
        case havePensionAdviser ~ srn =>
          for {
            details <- schemeDetailsConnector.getSchemeDetails("srn", srn)
            _ <- userAnswersCacheConnector.save(SchemeNameId, details.schemeDetails.name)
            _ <- userAnswersCacheConnector.save(IsMasterTrustId, details.schemeDetails.isMasterTrust)
            _ <- userAnswersCacheConnector.save(PSTRId, details.schemeDetails.pstr.getOrElse(""))
          } yield {
            Ok(declaration(appConfig, havePensionAdviser, details.schemeDetails.isMasterTrust, form))
          }
      }
  }

  def onSubmit(): Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          (HaveYouEmployedPensionAdviserId and IsMasterTrustId).retrieve.right.map {
            case havePensionAdviser ~ isMasterTrust =>
              Future.successful(BadRequest(declaration(appConfig, havePensionAdviser, isMasterTrust, formWithErrors)))
          },
        declaration => {
          PSTRId.retrieve.right.map { pstr =>
            acceptInviteAndRedirect(pstr, declaration)
          }
        }
      )
  }

  private def acceptInviteAndRedirect(pstr: String, declaration: Boolean)(implicit request: DataRequest[AnyContent]) = {
    val userAnswers = request.userAnswers

    invitationsCacheConnector.get(pstr, request.psaId).flatMap { invitations =>
      invitations.headOption match {
        case Some(invitation) =>
          HaveYouEmployedPensionAdviserId.retrieve.right.map { havePensionAdviser =>
            val acceptedInvitation = AcceptedInvitation(invitation.pstr, request.psaId, invitation.inviterPsaId, declaration,
              !havePensionAdviser, userAnswers.json.validate[PensionAdviserDetails].asOpt)

            invitationConnector.acceptInvite(acceptedInvitation).flatMap { _ =>
              invitationsCacheConnector.remove(invitation.pstr, request.psaId).map { _ =>
                Redirect(navigator.nextPage(DeclarationId, NormalMode, userAnswers))
              }
            }
          }
        case _ =>
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
    }
  }
}
