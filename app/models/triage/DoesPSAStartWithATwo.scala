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
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.Enumerable

sealed trait DoesPSAStartWithATwo

object DoesPSAStartWithATwo {

  case object Yes extends WithName("opt1") with DoesPSAStartWithATwo

  case object No extends WithName("opt2") with DoesPSAStartWithATwo

  val values: Seq[DoesPSAStartWithATwo] = Seq(
    Yes, No
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.map {
    value =>
      RadioItem(
        content = Text(messages(s"messages__doesPSAStartWithATwo__${value.toString}")),
        value=Some(value.toString))
  }

  implicit val enumerable: Enumerable[DoesPSAStartWithATwo] =
    Enumerable(values.map(v => v.toString -> v): _*)
}






