/*
 * Copyright 2020 HM Revenue & Customs
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

package models.invitations.psp

import play.api.libs.json._

case class DeAuthorise(
                        ceaseIDType: String,
                        ceaseNumber: String,
                        initiatedIDType: String,
                        initiatedIDNumber: String,
                        ceaseDate: String
                      )

object DeAuthorise {
  implicit lazy val writes: Writes[DeAuthorise] =
    (deAuthorise: DeAuthorise) => {
      val commonJson = Json.obj(
        "ceaseIDType" -> deAuthorise.ceaseIDType,
        "ceaseNumber" -> deAuthorise.ceaseNumber,
        "initiatedIDType" -> deAuthorise.initiatedIDType,
        "initiatedIDNumber" -> deAuthorise.initiatedIDNumber,
        "ceaseDate" -> deAuthorise.ceaseDate,
      )

      deAuthorise.ceaseIDType match {
        case "PSPID" =>
            deAuthorise.initiatedIDType match {
              case "PSAID" =>
                Json.obj("declarationCeasePSPDetails" ->
                  Json.obj("declarationBox1" -> "true")
                ) ++ commonJson
              case _ =>
                Json.obj("declarationCeasePSPDetails" ->
                  Json.obj("declarationBox2" -> "true")
                ) ++ commonJson
            }
        case _ =>
          commonJson
      }
    }

  implicit lazy val reads: Reads[DeAuthorise] =
    Json.reads[DeAuthorise]
}
