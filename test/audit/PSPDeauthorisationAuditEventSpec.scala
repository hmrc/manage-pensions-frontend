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

package audit

import java.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json
import utils.DateHelper


class PSPDeauthorisationAuditEventSpec extends WordSpec with MustMatchers {

  // scalastyle:off magic.number
  private val ceaseDate = LocalDate.of(2021, 3, 25)

  private val pspId = "pspId"
  private val psaId = "psaId"

  "details" must {
    "format in a valid way" in {
      val event = PSPDeauthorisationAuditEvent(
        ceaseDate = ceaseDate,
        psaId = psaId,
        pspId = pspId
      )


      val expected = Map(
          "ceaseDate" -> ceaseDate.format(DateHelper.auditFormatter),
          "initiatedIDNumber" -> psaId,
          "initiatedIDType" -> "PSAID",
          "ceaseNumber" -> pspId,
          "ceaseIDType"-> "PSPID",
          "declarationCeasePensionSchemePractitionerDetails" ->
            Json.stringify(Json.obj("declarationBox1" -> true))
        )

      event.details mustBe expected
    }
  }
}
