/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateHelper
import views.html.testOnly.date_test

import java.time.{Instant, LocalDate, ZoneOffset, ZonedDateTime}

@Singleton
class DateTestController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    view: date_test,
                                    val controllerComponents: MessagesControllerComponents
                                  ) extends FrontendBaseController with I18nSupport {

  def toInstant(date: (Int, Int, Int)): Instant =
    LocalDate.of(date._3, date._2, date._1).atStartOfDay().toInstant(ZoneOffset.UTC)

  def fromInstant(date: Instant): (Int, Int, Int) = {
    val zonedDateTime = ZonedDateTime.from(date)
    (zonedDateTime.getDayOfMonth, zonedDateTime.getMonthValue, zonedDateTime.getYear)
  }

  val dateMapping: Mapping[Instant] = (tuple(
    "day" -> number,
    "month" -> number,
    "year" -> number
  )).transform[Instant](toInstant, fromInstant)


  val form: Form[Option[Instant]] = Form("date" -> optional(dateMapping))

  def present: Action[AnyContent] = Action {
    implicit request =>
      Ok(view(form.fill(DateHelper.overriddenDate)))
  }

  def submit: Action[AnyContent] = Action {
    implicit request =>
      form.bindFromRequest().fold(
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
