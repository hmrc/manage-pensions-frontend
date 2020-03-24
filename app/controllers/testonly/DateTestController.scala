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

package controllers.testonly

import java.time.LocalDate

import com.google.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.DateHelper
import views.html.testOnly.date_test

import scala.concurrent.ExecutionContext

@Singleton
class DateTestController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    view: date_test,
                                    val controllerComponents: MessagesControllerComponents
                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Option[LocalDate]] = Form("date" -> optional(localDate("d MMMM yyyy")))

  def present: Action[AnyContent] = Action {
    implicit request =>
      Ok(view(form.fill(DateHelper.overriddenDate)))
  }

  def submit: Action[AnyContent] = Action {
    implicit request =>
      form.bindFromRequest.fold(
        invalidForm => {
          BadRequest(view(invalidForm))
        },
        date => {
          DateHelper.setDate(date)
          Redirect(controllers.testonly.routes.DateTestController.present())
        }
      )
  }
}