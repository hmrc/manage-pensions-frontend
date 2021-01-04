/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.remove.pspSelfRemoval

import java.time.LocalDate

import com.google.inject.Inject
import connectors.PspConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.remove.RemovePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.{SchemeNameId, SchemeSrnId}
import models.AuthEntity.PSP
import models.invitations.psp.DeAuthorise
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.remove.pspSelfRemoval.declaration

import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(override val messagesApi: MessagesApi,
                                      formProvider: RemovePspDeclarationFormProvider,
                                      auth: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      pspConnector: PspConnector,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: declaration
                                     )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {
  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (auth(PSP) andThen getData andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId).retrieve.right.map {
        case srn ~ schemeName =>
          Future.successful(Ok(view(form, schemeName, srn)))
      }
  }

  def onSubmit(): Action[AnyContent] = (auth(PSP) andThen getData andThen requireData).async {
      implicit request =>
        (SchemeSrnId and SchemeNameId and PSTRId).retrieve.right.map {
          case srn ~ schemeName ~ pstr =>
              form.bindFromRequest().fold(
                (formWithErrors: Form[Boolean]) =>
                  Future.successful(BadRequest(view(formWithErrors, schemeName, srn))),
                _ => {
                  val pspId = request.pspIdOrException.id
                  val deAuthModel: DeAuthorise = DeAuthorise("PSPID", pspId, "PSPID", pspId, LocalDate.now().toString)
                  pspConnector.deAuthorise(pstr, deAuthModel).map { _ =>
                    Redirect(controllers.remove.pspSelfRemoval.routes.ConfirmationController.onPageLoad())
                  }
                }
              )

        }
    }
}
