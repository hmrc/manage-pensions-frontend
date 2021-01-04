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

package models

import org.scalatest.MustMatchers
import org.scalatest.WordSpec

class SchemeReferenceNumberSpec extends WordSpec with MustMatchers {

  "SchemeReferenceNumber" must {

    "bind" in {
      SchemeReferenceNumber.srnPathBindable.bind("schemeReferenceNumber", "S0987654321") mustBe Right(SchemeReferenceNumber("S0987654321"))
    }

    "not bind" in {
      SchemeReferenceNumber.srnPathBindable.bind("schemeReferenceNumber", "Invalid") mustBe Left("SchemeReferenceNumber binding failed")
    }

  }

}
