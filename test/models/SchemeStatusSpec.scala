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

package models

import org.scalacheck.Gen
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class SchemeStatusSpec extends FlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  "SchemeStatus" should "correctly configure Pending" in {

    SchemeStatus.Pending.value shouldBe "Pending"
    SchemeStatus.Pending.pending shouldBe true

    SchemeStatus.PendingInfoRequired.value shouldBe "Pending Info Required"
    SchemeStatus.PendingInfoRequired.pending shouldBe true

    SchemeStatus.PendingInfoReceived.value shouldBe "Pending Info Received"
    SchemeStatus.PendingInfoReceived.pending shouldBe true

    val pending = SchemeStatus.statuses.filter(_.pending)
    pending.size shouldBe 3

    val statuses = Gen.oneOf(pending)

    forAll(statuses) {
      status =>
        status.rejected shouldBe false
        status.canRemovePsa shouldBe false
    }

  }

  it should "correctly configure Rejected" in {

    SchemeStatus.Rejected.value shouldBe "Rejected"
    SchemeStatus.Rejected.rejected shouldBe true
    SchemeStatus.Rejected.canRemovePsa shouldBe true

    SchemeStatus.RejectedUnderAppeal.value shouldBe "Rejected Under Appeal"
    SchemeStatus.RejectedUnderAppeal.rejected shouldBe true
    SchemeStatus.RejectedUnderAppeal.canRemovePsa shouldBe false

    SchemeStatus.statuses.count(_.rejected) shouldBe 2
  }

  it should "correctly configure Open" in {

    SchemeStatus.Open.value shouldBe "Open"

    SchemeStatus.Open.canRemovePsa shouldBe true
    SchemeStatus.Open.pending shouldBe false
    SchemeStatus.Open.rejected shouldBe false

  }

  it should "correctly configure Deregistered" in {

    SchemeStatus.Deregistered.value shouldBe "Deregistered"

    SchemeStatus.Deregistered.canRemovePsa shouldBe false
    SchemeStatus.Deregistered.pending shouldBe false
    SchemeStatus.Deregistered.rejected shouldBe false

  }

  it should "correctly configure Wound-up" in {

    SchemeStatus.WoundUp.value shouldBe "Wound-up"

    SchemeStatus.WoundUp.canRemovePsa shouldBe true
    SchemeStatus.WoundUp.pending shouldBe false
    SchemeStatus.WoundUp.rejected shouldBe false

  }

}
