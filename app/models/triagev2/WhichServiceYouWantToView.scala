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

package models.triagev2

import models.WithName
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.Enumerable

import scala.language.implicitConversions

sealed trait WhichServiceYouWantToView

object WhichServiceYouWantToView {

  case object ManagingPensionSchemes extends WithName("opt1") with WhichServiceYouWantToView
  case object PensionSchemesOnline extends WithName("opt2") with WhichServiceYouWantToView
  case object IamUnsure extends WithName("opt3") with WhichServiceYouWantToView


  def values: Seq[WhichServiceYouWantToView] =
    Seq(ManagingPensionSchemes, PensionSchemesOnline, IamUnsure)


  def options(role: String)(implicit messages: Messages): Seq[RadioItem] = values.map {
    value =>
      RadioItem(
        content = Text(messages(s"messages__whichServiceYouWantToView__${value.toString}")),
        value = Some(value.toString),
        hint = value.toString match {
          case  "opt1" | "opt2" => Some(Hint(content = Text(messages(s"messages__whichServiceYouWantToView__${value.toString}__${role}__hint"))))
          case _ => None
        }
      )
  }

  implicit def enumerable(role: String): Enumerable[WhichServiceYouWantToView] =
    Enumerable(values.map(v => v.toString -> v): _*)

  implicit val format: Format[WhichServiceYouWantToView] = new Format[WhichServiceYouWantToView] {

    override def writes(o: WhichServiceYouWantToView): JsValue = JsString(o.toString)

    override def reads(json: JsValue): JsResult[WhichServiceYouWantToView] = json match {
      case JsString(str) =>
        values.find(_.toString == str) match {
          case Some(value) => JsSuccess(value)
          case None => JsError(s"Invalid value: $str")
        }
      case _ => JsError("Expected a JSON string")
    }
  }
}


