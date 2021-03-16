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
import utils.Enumerable
import utils.InputOption

sealed trait WhatRole


object WhatRole {

  case object PSA extends WithName("PSA") with WhatRole

  case object PSP extends WithName("PSP") with WhatRole

  case object NotRegistered extends WithName("opt3") with WhatRole

  def fromString(value: String): WhatRole = value match {
    case "PSA" => PSA
    case "PSP" => PSP
    case _ => NotRegistered
  }

  val values: Seq[WhatRole] = Seq(PSA, PSP, NotRegistered)

  val options: Seq[InputOption] = values.map {
    value =>
      InputOption(value.toString, s"messages__whatRole__${value.toString}")
  }

  implicit val enumerable: Enumerable[WhatRole] =
    Enumerable(values.map(v => v.toString -> v): _*)
}


