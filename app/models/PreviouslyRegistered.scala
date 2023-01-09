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

package models

import play.api.libs.json._
import utils.Enumerable

sealed trait PreviouslyRegistered

object PreviouslyRegistered {

  case object PreviouslyRegisteredButNotLoggedIn extends WithName("previouslyRegisteredButNotLoggedIn") with PreviouslyRegistered

  case object PreviouslyRegisteredButStoppedBeingAdministrator extends WithName("previouslyRegisteredButStoppedBeingAdministrator") with PreviouslyRegistered

  case object NotPreviousRegistered extends WithName("notPreviouslyRegistered") with PreviouslyRegistered

  val values: Seq[PreviouslyRegistered] = Seq(
    PreviouslyRegisteredButNotLoggedIn, PreviouslyRegisteredButStoppedBeingAdministrator, NotPreviousRegistered
  )

  private val mappings: Map[String, PreviouslyRegistered] = values.map(v => (v.toString, v)).toMap

  implicit val reads: Reads[PreviouslyRegistered] =
    JsPath.read[String].flatMap {
      case aop if mappings.keySet.contains(aop) => Reads(_ => JsSuccess(mappings.apply(aop)))
      case invalidValue => Reads(_ => JsError(s"Invalid previously registered type: $invalidValue"))
    }

  implicit lazy val writes: Writes[PreviouslyRegistered] = (aop: PreviouslyRegistered) => JsString(aop.toString)

  implicit val enumerable: Enumerable[PreviouslyRegistered] = Enumerable(values.map(v => v.toString -> v): _*)
}
