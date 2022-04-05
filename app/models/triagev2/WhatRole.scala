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

package models.triagev2

import models.WithName
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.Enumerable

sealed trait WhatRole


object WhatRole {

  case object PSA extends WithName("administrator") with WhatRole

  case object PSP extends WithName("practitioner") with WhatRole

  case object NotRegistered extends WithName("opt3") with WhatRole

  def fromString(value: String): WhatRole = value match {
    case "administrator" => PSA
    case "practitioner" => PSP
    case _ => NotRegistered
  }

  val values: Seq[WhatRole] = Seq(PSA, PSP, NotRegistered)

  def options(implicit messages: Messages): Seq[RadioItem] = values.map {
    value =>
      RadioItem(
        content = Text(messages(s"messages__whatRole__${value.toString}")),
        value=Some(value.toString))
  }

  implicit val enumerable: Enumerable[WhatRole] =
    Enumerable(values.map(v => v.toString -> v): _*)
}


