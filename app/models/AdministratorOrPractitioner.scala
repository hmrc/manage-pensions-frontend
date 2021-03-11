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

import play.api.libs.json._
import utils.{Enumerable, InputOption}

sealed trait AdministratorOrPractitioner

object AdministratorOrPractitioner {

  case object Administrator extends WithName("administrator") with AdministratorOrPractitioner

  case object Practitioner extends WithName("practitioner") with AdministratorOrPractitioner

  val values: Seq[AdministratorOrPractitioner] = Seq(
    Administrator, Practitioner
  )

  val mappings: Map[String, AdministratorOrPractitioner] = values.map(v => (v.toString, v)).toMap

  val options: Seq[InputOption] = values.map {
    value =>
      InputOption(value.toString, s"messages__administratorOrPractitioner__${value.toString}", hint=Set(s"messages__administratorOrPractitioner__${value.toString}_hint"))
  }

  implicit val reads: Reads[AdministratorOrPractitioner] = {

    (JsPath \ "name").read[String].flatMap {
      case aop if mappings.keySet.contains(aop) =>
        Reads(_ => JsSuccess(mappings.apply(aop)))

      case _ => Reads(_ => JsError("Invalid Scheme Type"))
    }
  }

  implicit lazy val writes = new Writes[AdministratorOrPractitioner] {
    def writes(o: AdministratorOrPractitioner) = {
      o match {
        case s if mappings.keySet.contains(s.toString) =>
          Json.obj("name" -> s.toString)
      }
    }
  }

  implicit val enumerable: Enumerable[AdministratorOrPractitioner] =
    Enumerable(values.map(v => v.toString -> v): _*)
}






