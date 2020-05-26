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

package controllers.triage

import config.FrontendAppConfig
import forms.triage.DoesPSTRStartWithTwoFormProvider
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.triage.doesPSTRStartWithTwo

import scala.concurrent.{ExecutionContext, Future}

class DoesPSTRStartWithTwoController @Inject()(
                                                appConfig: FrontendAppConfig,
                                                override val messagesApi: MessagesApi,
                                                formProvider: DoesPSTRStartWithTwoFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: doesPSTRStartWithTwo
                                              )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Ok(view(form)))
  }

  def onSubmit: Action[AnyContent] = Action.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        _ => {
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
        }
      )
  }
}
