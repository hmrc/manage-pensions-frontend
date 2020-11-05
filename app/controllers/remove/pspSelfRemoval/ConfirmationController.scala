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

package controllers.remove.pspSelfRemoval

import com.google.inject.Inject
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.remove.RemovePspDeclarationFormProvider
import models.AuthEntity.PSP
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.remove.pspSelfRemoval.declaration

import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(override val messagesApi: MessagesApi,
                                       formProvider: RemovePspDeclarationFormProvider,
                                       auth: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: declaration
                                     )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {
  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (auth(PSP) andThen getData andThen requireData) {
    implicit request =>
      Ok
  }

  def onSubmit(): Action[AnyContent] = (auth(PSP) andThen getData andThen requireData) {
      implicit request =>
        Ok
    }
}
