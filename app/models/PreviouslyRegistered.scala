/*
 * Copyright 2022 HM Revenue & Customs
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

package models

import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.Aliases.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.Enumerable

sealed trait PreviouslyRegistered

object PreviouslyRegistered {

  case object YesNotLoggedIn extends WithName("yesNotLoggedIn") with PreviouslyRegistered

  case object YesStopped extends WithName("yesStopped") with PreviouslyRegistered

  case object No extends WithName("no") with PreviouslyRegistered

  val values: Seq[PreviouslyRegistered] = Seq(
    YesNotLoggedIn, YesStopped, No
  )

  private val mappings: Map[String, PreviouslyRegistered] = values.map(v => (v.toString, v)).toMap

  private def seqInputOption(messageKey:String, includeHints:Boolean, values: Seq[PreviouslyRegistered])(implicit messages: Messages): Seq[RadioItem] = {
    values.map { value =>
      val optionHint = if(includeHints) Some(Hint(Some(s"messages__${messageKey}__${value.toString}_hint"))) else None
      optionHint match {
        case None => RadioItem(
          content = Text(messages(s"messages__${messageKey}__${value.toString}")),
          value = Some(value.toString))
        case Some(h) => RadioItem(
          content = Text(messages(s"messages__${messageKey}__${value.toString}")),
          value = Some(value.toString),
          hint = Some(h))
      }
    }
  }

  implicit val reads: Reads[PreviouslyRegistered] =
    JsPath.read[String].flatMap {
      case aop if mappings.keySet.contains(aop) => Reads(_ => JsSuccess(mappings.apply(aop)))
      case invalidValue => Reads(_ => JsError(s"Invalid previously registered type: $invalidValue"))
    }

  implicit lazy val writes: Writes[PreviouslyRegistered] = (aop: PreviouslyRegistered) => JsString(aop.toString)

  implicit val enumerable: Enumerable[PreviouslyRegistered] = Enumerable(values.map(v => v.toString -> v): _*)
}
