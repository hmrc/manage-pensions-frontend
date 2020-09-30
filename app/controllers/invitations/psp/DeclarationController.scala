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
import config.FrontendAppConfig
import connectors.PspConnector
import connectors.scheme.{ListOfSchemesConnector, SchemeDetailsConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.psp.DeclarationFormProvider
import identifiers.SchemeSrnId
import identifiers.invitations.PSTRId
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import models.invitations.psp.ClientReference
import models.requests.DataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDetailsService
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.invitations.psp.declaration

import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
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

  def onPageLoad(): Action[AnyContent] = (auth andThen getData andThen requireData) {
    implicit request =>
          Ok(view(form))
  }

  def onSubmit(): Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        _ =>

          inviteAndRedirect()
      )
   }

  private def inviteAndRedirect()(implicit request: DataRequest[AnyContent]): Future[Result] = {
    (SchemeSrnId and PspNameId and PspId and PspClientReferenceId).retrieve.right.map {
      case srn ~ pspName ~ pspId ~ pspCR =>
        listOfSchemesConnector.getListOfSchemes(request.psaId.id).map(_.right.map(list =>
          schemeDetailsService.pstr(srn, list).map {pstr =>
            pspConnector.authorisePsp(pstr, pspName, pspId, getClientReference(pspCR)).map { x =>
              Future.successful(Redirect(routes.ConfirmationController.onPageLoad()))

        }.recoverWith(Redirect(routes.PspDoesNotMatchController.onPageLoad()))}
        ).left.map(Redirect(controllers.routes.SessionExpiredController.onPageLoad())))
    }
  }

  private def getClientReference(answer: ClientReference): Option[String] = answer match {
    case ClientReference.HaveClientReference(reference) => Some(reference)
    case ClientReference.NoClientReference => None
  }
}
