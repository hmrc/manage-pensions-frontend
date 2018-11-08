/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}
import testhelpers.CommonBuilders.mockSchemeDetails

class PsaSchemeDetailsSpec extends WordSpec with MustMatchers with GeneratorDrivenPropertyChecks {

  import PsaSchemeDetailsSpec._

  "SchemeDetailsController.canRemovePsa" must {

    "return false if there are no other PSAs for all values of status" in {

      val statuses = Gen.oneOf(SchemeStatus.statuses)

      forAll(statuses) {
        status =>
          val scheme = testScheme(status, testPsa1)
          PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe false
      }

    }

    "return true if there are other PSAs and the scheme has a status of Open" in {

      val scheme = testScheme(SchemeStatus.Open, testPsa1, testPsa2)
      PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe true

    }

    "return false if there are other PSAs and the scheme has a status of Pending" in {

      val statuses = Gen.oneOf(SchemeStatus.statuses.filter(_.pending))

      forAll(statuses) {
        status =>
          val scheme = testScheme(status, testPsa1, testPsa2)
          PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe false
      }

    }

    "return false if there are other PSAs and the scheme has a status of Rejected" in {

      val statuses = Gen.oneOf(SchemeStatus.statuses.filter(_.rejected))

      forAll(statuses) {
        status =>
          val scheme = testScheme(status, testPsa1, testPsa2)
          PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe false
      }

    }

    "return true if there are other PSAs and the scheme has a status of Wound-Up" in {

      val scheme = testScheme(SchemeStatus.WoundUp, testPsa1, testPsa2)
      PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe true

    }

    "return false if there are other PSAs and the scheme has a status of De-Registered" in {

      val scheme = testScheme(SchemeStatus.Deregistered, testPsa1, testPsa2)
      PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe false

    }

  }

}

object PsaSchemeDetailsSpec {

  def testScheme(status: SchemeStatus, psas: PsaDetails*): PsaSchemeDetails =
    PsaSchemeDetails(
      mockSchemeDetails.copy(status = status.value),
      None,
      None,
      Some(psas)
    )

  val testPsaId1 = "test-psa-id-1"
  val testPsa1: PsaDetails = PsaDetails(testPsaId1, Some("test-org-1"), None)

  val testPsaId2 = "test-psa-id-2"
  val testPsa2: PsaDetails = PsaDetails(testPsaId2, Some("test-org-2"), None)

}
