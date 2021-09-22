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

package models.triage

import models.WithName
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.Enumerable

sealed trait DoesPSTRStartWithATwo

object DoesPSTRStartWithATwo {

  case object Yes extends WithName("opt1") with DoesPSTRStartWithATwo

  case object No extends WithName("opt2") with DoesPSTRStartWithATwo

  val values: Seq[DoesPSTRStartWithATwo] = Seq(
    Yes, No
  )

  def options(noHint: Option[String])(implicit messages: Messages): Seq[RadioItem] = values.map {
    value =>
      val hintText: Option[Hint] = (value, noHint) match {
        case (No, Some(hint)) => Some(Hint(content = Text(hint)))
        case _ => None
      }
      RadioItem(
        content = Text(messages(s"messages__doesPSTRStartWithATwo__${value.toString}")),
        value=Some(value.toString),
        hint = hintText)
  }

  implicit val enumerable: Enumerable[DoesPSTRStartWithATwo] =
    Enumerable(values.map(v => v.toString -> v): _*)
}






