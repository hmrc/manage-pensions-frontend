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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import utils.DateHelper

import java.time.LocalDate


class PSPDeauthorisationByPSAAuditEventSpec extends AnyWordSpec with Matchers {

  // scalastyle:off magic.number
  private val ceaseDate = LocalDate.of(2021, 3, 25)

  private val pspId = "pspId"
  private val psaId = "psaId"
  private val pstr = "pstr"

  "details" must {
    "format in a valid way" in {
      val event = PSPDeauthorisationByPSAAuditEvent(
        ceaseDate = ceaseDate,
        psaId = psaId,
        pspId = pspId,
        pstr = pstr
      )


      val expected = Json.obj(
        "ceaseDate" -> ceaseDate.format(DateHelper.auditFormatter),
        "initiatedIDNumber" -> psaId,
        "initiatedIDType" -> "PSAID",
        "ceaseNumber" -> pspId,
        "ceaseIDType" -> "PSPID",
        "pensionSchemeTaxReference" -> pstr,
        "declarationCeasePensionSchemePractitionerDetails" ->
          Json.obj("declarationBox1" -> true)
      )

      event.details mustBe expected
    }
  }
}
