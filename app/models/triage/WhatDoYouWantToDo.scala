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

package models.triage

import models.WithName
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.Enumerable

import scala.language.implicitConversions

sealed trait WhatDoYouWantToDo


object WhatDoYouWantToDo {

  case object ManageExistingScheme extends WithName("opt1") with WhatDoYouWantToDo
  case object CheckTheSchemeStatus extends WithName("opt2") with WhatDoYouWantToDo
  case object Invite extends WithName("opt3") with WhatDoYouWantToDo
  case object BecomeAnAdmin extends WithName("opt4") with WhatDoYouWantToDo
  case object AuthorisePsp extends WithName("opt5") with WhatDoYouWantToDo
  case object UpdateSchemeInformation extends WithName("opt6") with WhatDoYouWantToDo
  case object ChangeAdminDetails extends WithName("opt7") with WhatDoYouWantToDo
  case object RegisterScheme extends WithName("opt8") with WhatDoYouWantToDo
  case object ChangePspDetails extends WithName("opt9") with WhatDoYouWantToDo
  case object DeauthYourself extends WithName("opt10") with WhatDoYouWantToDo

  def values(role: String): Seq[WhatDoYouWantToDo] =
    if(role.equals("PSA")) {
      Seq(ManageExistingScheme, CheckTheSchemeStatus, Invite, BecomeAnAdmin, AuthorisePsp, UpdateSchemeInformation, ChangeAdminDetails, RegisterScheme)
    } else {
      Seq(ManageExistingScheme, ChangePspDetails, DeauthYourself)
    }

  def options(role: String)(implicit messages: Messages): Seq[RadioItem] = values(role).map {
    value =>
      RadioItem(
        content = Text(messages(s"messages__whatDoYouWantToDo__${value.toString}")),
        value = Some(value.toString)
      )
  }

  implicit def enumerable(role: String): Enumerable[WhatDoYouWantToDo] =
    Enumerable(values(role).map(v => v.toString -> v): _*)
}


