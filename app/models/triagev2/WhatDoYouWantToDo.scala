/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.language.implicitConversions

sealed trait WhatDoYouWantToDo


object WhatDoYouWantToDo {

  case object ManageExistingScheme extends WithName("opt1") with WhatDoYouWantToDo
  case object FileAccountingForTaxReturn extends WithName("opt2") with WhatDoYouWantToDo
  case object FilePensionSchemeReturn extends WithName("opt3") with WhatDoYouWantToDo
  case object FileEventReport extends WithName("opt4") with WhatDoYouWantToDo


  def values: Seq[WhatDoYouWantToDo] =
      Seq(ManageExistingScheme, FileAccountingForTaxReturn, FilePensionSchemeReturn,FileEventReport)


  def options(role: String)(implicit messages: Messages): Seq[RadioItem] = values.map {
    value =>
      RadioItem(
        content = Text(messages(s"messages__whatDoYouWantToDo__v2__${value.toString}")),
        value = Some(value.toString)
      )
  }

  implicit def enumerable(role: String): Enumerable[WhatDoYouWantToDo] =
    Enumerable(values.map(v => v.toString -> v): _*)
}


