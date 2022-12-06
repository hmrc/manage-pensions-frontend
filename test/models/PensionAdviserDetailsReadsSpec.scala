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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.{JsValue, Json}
import testhelpers.InvitationBuilder

class PensionAdviserDetailsReadsSpec extends AsyncWordSpec with Matchers {

  import PensionAdviserDetailsReadsSpec._

  "PensionAdviserDetails" must {
    val result = inputJson.as[PensionAdviserDetails](PensionAdviserDetails.userAnswerReads)
    "read the adviser name" in {
      result.name mustBe InvitationBuilder.pensionAdviser.name
    }

    "read the adviser address" in {
      result.addressDetail mustBe InvitationBuilder.pensionAdviser.addressDetail
    }

    "read the adviser email" in {
      result.email mustBe InvitationBuilder.pensionAdviser.email
    }
  }
}

object PensionAdviserDetailsReadsSpec {

  val inputJson: JsValue = Json.parse(
    """ {
      |"adviserName":"test adviser",
      |"adviserEmail":"test@test.com",
      |"adviserAddress":{"addressLine1":"line 1","addressLine2":"line 2","addressLine3":"line 3","addressLine4":"line 4","postcode":"AB11AB","country":"GB"}}
    """.stripMargin)
}
