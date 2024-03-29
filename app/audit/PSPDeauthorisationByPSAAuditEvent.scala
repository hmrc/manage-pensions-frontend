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

package audit

import java.time.LocalDate
import play.api.libs.json.{JsObject, Json}
import utils.DateHelper

case class PSPDeauthorisationByPSAAuditEvent(
                                              ceaseDate: LocalDate,
                                              psaId: String,
                                              pspId: String,
                                              pstr: String
                                            ) extends ExtendedAuditEvent {
  override def auditType: String = "PensionSchemeAdministratorDeauthorisePractitioner"

  override def details: JsObject = Json.obj(
    "ceaseDate" -> ceaseDate.format(DateHelper.auditFormatter),
    "ceaseNumber" -> pspId,
    "initiatedIDType" -> "PSAID",
    "initiatedIDNumber" -> psaId,
    "ceaseIDType" -> "PSPID",
    "pensionSchemeTaxReference" -> pstr,
    "declarationCeasePensionSchemePractitionerDetails" -> Json.obj("declarationBox1" -> true)
  )
}
