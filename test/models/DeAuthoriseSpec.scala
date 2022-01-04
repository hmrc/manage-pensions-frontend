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

import connectors.PspConnectorSpec._
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class DeAuthoriseSpec extends WordSpec with MustMatchers {

  "DeAuthorise" must {
    "not write declarationCeasePSPDetails for PSA deAuth PSA" in {
      val result: JsValue = Json.toJson(psaDeAuthPsa)

      (result \ "declarationCeasePSPDetails").asOpt[String] mustBe None
    }

    "write declarationCeasePSPDetails declarationBox1 for PSA deAuth PSP" in {
      val result: JsValue = Json.toJson(psaDeAuthPsp)

      (result \ "declarationCeasePSPDetails" \ "declarationBox1").asOpt[Boolean] mustBe Some(true)
      (result \ "declarationCeasePSPDetails" \ "declarationBox2").asOpt[Boolean] mustBe None
    }

    "write declarationCeasePSPDetails declarationBox2 for PSP deAuth PSP" in {
      val result: JsValue = Json.toJson(pspDeAuthPsp)

      (result \ "declarationCeasePSPDetails" \ "declarationBox1").asOpt[Boolean] mustBe None
      (result \ "declarationCeasePSPDetails" \ "declarationBox2").asOpt[Boolean] mustBe Some(true)
    }
  }
}
