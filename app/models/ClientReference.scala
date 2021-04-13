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
import utils.InputOption

sealed trait ClientReference
object ClientReference {

  case class HaveClientReference(reference: String) extends WithName("true") with ClientReference
  case object NoClientReference extends WithName("false") with ClientReference

  def options: Seq[InputOption] = {
    Seq(

      InputOption(
        value = "true",
        label = s"site.yes",
        dataTarget = Some("value_reference-form"),
        hint = Set.empty
      ),
      InputOption(
        value = "false",
        label = s"site.no",
        hint = Set.empty
      )
    )
  }

  implicit val reads: Reads[ClientReference] = {

    (JsPath \ "hasReference").read[String].flatMap {

      case "true" =>
        (JsPath \ "reference").read[String]
          .map[ClientReference](HaveClientReference.apply)
          .orElse(Reads[ClientReference](_ => JsError("ClientReference expected")))

      case _ => Reads(_ => JsSuccess(NoClientReference))
    }
  }

  implicit lazy val writes: Writes[ClientReference] = {
    case ClientReference.HaveClientReference(reference) =>
      Json.obj("hasReference" -> "true", "reference" -> reference)
    case _ =>
      Json.obj("hasReference" -> "false")
  }

}


