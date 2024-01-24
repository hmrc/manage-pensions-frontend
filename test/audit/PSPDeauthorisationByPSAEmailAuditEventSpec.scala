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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PSPDeauthorisationByPSAEmailAuditEventSpec
  extends AnyFlatSpec
    with Matchers {

  "PSPDeauthorisationByPSAEmailAuditEvent" should "output the correct map of data" in {

    val event = PSPDeauthorisationByPSAEmailAuditEvent(
      psaId = "pensionSchemeAdministratorId",
      pspId = "pensionSchemePractitionerId",
      pstr = "pensionSchemeTaxReference",
      emailAddress = "email@address"
    )

    val expected = Map(
      "pensionSchemeAdministratorId" -> "pensionSchemeAdministratorId",
      "pensionSchemePractitionerId" -> "pensionSchemePractitionerId",
      "pensionSchemeTaxReference" -> "pensionSchemeTaxReference",
      "emailAddress" -> "email@address",
      "event" -> "Sent"
    )

    event.auditType shouldBe "PensionSchemePractitionerDeauthorisedEmailEvent"
    event.details shouldBe expected
  }
}
