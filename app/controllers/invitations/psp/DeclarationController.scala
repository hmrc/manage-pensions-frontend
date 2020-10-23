/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.invitations.psp

import com.google.inject.Inject
import connectors.scheme.ListOfSchemesConnector
import connectors.{ActiveRelationshipExistsException, PspConnector}
import controllers.Retrievals
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import forms.invitations.psp.DeclarationFormProvider
import identifiers.SchemeSrnId
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import models.invitations.psp.ClientReference
import models.requests.DataRequest
import play.api.data.Form
import services.SchemeDetailsService
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.invitations.psp.declaration

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class DeclarationController @Inject()( override val messagesApi: MessagesApi,
                                       formProvider: DeclarationFormProvider,
                                       auth: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       pspConnector: PspConnector,
                                       listOfSchemesConnector: ListOfSchemesConnector,
                                       schemeDetailsService: SchemeDetailsService,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: declaration
                                     )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {
  val form: Form[Boolean] = formProvider()
  val sessionExpired: Future[Result] = Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))

  def onPageLoad(): Action[AnyContent] = (auth() andThen getData andThen requireData) {
    implicit request =>
          Ok(view(form))
  }

  def onSubmit(): Action[AnyContent] = (auth() andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        _ => inviteAndRedirect()
      )
   }

  private def inviteAndRedirect()(implicit request: DataRequest[AnyContent]): Future[Result] =
    (SchemeSrnId and PspNameId and PspId and PspClientReferenceId).retrieve.right.map {
      case srn ~ pspName ~ pspId ~ pspCR =>
        getPstr(srn).flatMap {
          case Some(pstr) =>
            pspConnector.authorisePsp(pstr, pspName, pspId, getClientReference(pspCR)).map { _ =>
              Redirect(routes.ConfirmationController.onPageLoad())
            } recoverWith {
              case _: ActiveRelationshipExistsException =>
                Future.successful(Redirect(controllers.invitations.psp.routes.AlreadyAssociatedWithSchemeController.onPageLoad()))
            }
          case _ => sessionExpired
        }
    }.left.map(_ => sessionExpired)


  private def getPstr(srn: String)(implicit request: DataRequest[AnyContent]): Future[Option[String]] =
    listOfSchemesConnector.getListOfSchemes(request.psaIdOrException.id).map {
      case Right(list) => schemeDetailsService.pstr(srn, list)
      case _ => None
    }

  private def getClientReference(answer: ClientReference): Option[String] = answer match {
    case ClientReference.HaveClientReference(reference) => Some(reference)
    case ClientReference.NoClientReference => None
  }
}
