/*
 * Copyright 2019 HM Revenue & Customs
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

import models.SchemeStatus.WoundUp
import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}
import testhelpers.CommonBuilders.mockSchemeDetails

class PsaSchemeDetailsSpec extends WordSpec with MustMatchers with GeneratorDrivenPropertyChecks {

  import PsaSchemeDetailsSpec._

  "SchemeDetailsController.canRemovePsa" must {

    "return false " when {
      "there are no other PSAs for all values of status except for WoundUp" in {
        val statuses = Gen.oneOf(SchemeStatus.statuses.filterNot(_ == WoundUp))

        forAll(statuses) {
          status =>
            val scheme = testScheme(status, testPsa1())
            PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe false
        }
      }

      "there are other PSAs and the scheme has a status of Pending" in {
        val statuses = Gen.oneOf(SchemeStatus.statuses.filter(_.pending))

        forAll(statuses) {
          status =>
            val scheme = testScheme(status, testPsa1(), testPsa2)
            PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe false
        }
      }

      "there are other PSAs and the scheme has a status of Rejected" in {
        val statuses = Gen.oneOf(SchemeStatus.statuses.filter(_.rejected))

        forAll(statuses) {
          status =>
            val scheme = testScheme(status, testPsa1(), testPsa2)
            PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe false
        }
      }

      "there are other PSAs and the scheme has a status of De-Registered" in {
        val scheme = testScheme(SchemeStatus.Deregistered, testPsa1(), testPsa2)
        PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe false
      }

      "there are other PSAs with correct scheme status of Open but the PSA is removing on the same day as association" in {
        val currentDate = LocalDate.now().toString
        val scheme = testScheme(SchemeStatus.Open, testPsa1(currentDate), testPsa2)
        PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe false
      }
    }

    "return true" when {
      "there are other PSAs and the scheme has a status of Open and not removing on the same day as association" in {
        val scheme = testScheme(SchemeStatus.Open, testPsa1(), testPsa2)
        PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe true
      }

      "there are no other PSAs and the scheme has a status of Wound-Up and not removing on the same day as association" in {
        val scheme = testScheme(SchemeStatus.WoundUp, testPsa1())
        PsaSchemeDetails.canRemovePsa(testPsaId1, scheme) mustBe true
      }
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

  def testPsa1(date: String = "2018-10-01"): PsaDetails = PsaDetails(testPsaId1, Some("test-org-1"), None, Some(date))

  val testPsaId2 = "test-psa-id-2"
  val testPsa2: PsaDetails = PsaDetails(testPsaId2, Some("test-org-2"), None, None)
}
