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

package models

import utils.{Enumerable, InputOption}

sealed trait AdministratorOrPractitioner

object AdministratorOrPractitioner {

  case object Administrator extends WithName("administrator") with AdministratorOrPractitioner

  case object Practitioner extends WithName("practitioner") with AdministratorOrPractitioner

  val values: Seq[AdministratorOrPractitioner] = Seq(
    Administrator, Practitioner
  )

  val options: Seq[InputOption] = values.map {
    value =>
      InputOption(value.toString, s"messages__administratorOrPractitioner__${value.toString}")
  }

  implicit val enumerable: Enumerable[AdministratorOrPractitioner] =
    Enumerable(values.map(v => v.toString -> v): _*)
}






