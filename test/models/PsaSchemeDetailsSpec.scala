/*
 * Copyright 2022 HM Revenue & Customs
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

import models.SchemeStatus.{Deregistered, Rejected, WoundUp}
import models.psa.{PsaDetails, PsaSchemeDetails}
import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.time.LocalDate

class PsaSchemeDetailsSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  import PsaSchemeDetailsSpec._

  "SchemeDetailsController.canRemovePsaVariations" must {

    "return false " when {
      "there are NO other PSAs administering the scheme and the scheme status is NOT WoundUp, Rejected or Deregistered" in {
        val statusesWhereSoleOwnerCanBeRemoved = Set[SchemeStatus](WoundUp, Rejected, Deregistered)

        val statuses = Gen.oneOf(SchemeStatus.statuses.filterNot(s => statusesWhereSoleOwnerCanBeRemoved.contains(s)))

        forAll(statuses) {
          status =>
            PsaSchemeDetails.canRemovePsaVariations(testPsaId1, Seq(testPsa1()), status.value) mustBe false
        }
      }

      "there are other PSAs and the scheme has a status of Pending" in {
        val statuses = Gen.oneOf(SchemeStatus.statuses.filter(_.pending))

        forAll(statuses) {
          status =>
            PsaSchemeDetails.canRemovePsaVariations(testPsaId1, Seq(testPsa1(), testPsa2), status.value) mustBe false
        }
      }

      "there are other PSAs and the scheme has a status of Rejected Under Appeal" in {
        PsaSchemeDetails.canRemovePsaVariations(
          testPsaId1, Seq(testPsa1(), testPsa2),
          SchemeStatus.RejectedUnderAppeal.value) mustBe false
      }

      "there are other PSAs and the scheme has a status of Rejected" in {
        PsaSchemeDetails.canRemovePsaVariations(
          testPsaId1, Seq(testPsa1(), testPsa2),
          SchemeStatus.Rejected.value) mustBe true
      }

      "there are other PSAs with correct scheme status of Open but the PSA is removing on the same day as association" in {
        val currentDate = LocalDate.now().toString
        PsaSchemeDetails.canRemovePsaVariations(testPsaId1, Seq(testPsa1(currentDate), testPsa2), SchemeStatus.Open.value) mustBe false
      }
    }

    "return true" when {
      "there are other PSAs and the scheme has a status of Open and not removing on the same day as association" in {
        PsaSchemeDetails.canRemovePsaVariations(testPsaId1, Seq(testPsa1(), testPsa2), SchemeStatus.Open.value) mustBe true
      }

      "there are no other PSAs and the scheme has a status of Wound-Up and not removing on the same day as association" in {
        PsaSchemeDetails.canRemovePsaVariations(testPsaId1, Seq(testPsa1()), SchemeStatus.WoundUp.value) mustBe true
      }

      "there are no other PSAs and the scheme has a status of Rejected and not removing on the same day as association" in {
        PsaSchemeDetails.canRemovePsaVariations(testPsaId1, Seq(testPsa1()), SchemeStatus.Rejected.value) mustBe true
      }

      "there are no other PSAs and the scheme has a status of Deregistered and not removing on the same day as association" in {
        PsaSchemeDetails.canRemovePsaVariations(testPsaId1, Seq(testPsa1()), SchemeStatus.Deregistered.value) mustBe true
      }
    }
  }

}

object PsaSchemeDetailsSpec {

  val testPsaId1 = "test-psa-id-1"

  def testPsa1(date: String = "2018-10-01"): PsaDetails = PsaDetails(testPsaId1, Some("test-org-1"), None, Some(date))

  val testPsaId2 = "test-psa-id-2"
  val testPsa2: PsaDetails = PsaDetails(testPsaId2, Some("test-org-2"), None, None)
}
