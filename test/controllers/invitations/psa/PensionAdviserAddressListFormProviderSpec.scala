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

package controllers.invitations.psa

import forms.FormSpec
import forms.invitations.psa.PensionAdviserAddressListFormProvider
import org.scalatest.wordspec.AnyWordSpec

class PensionAdviserAddressListFormProviderSpec extends AnyWordSpec with FormSpec {

  val validData: Map[String, String] = Map(
    "value" -> "0"
  )

  val form = new PensionAdviserAddressListFormProvider()(Seq(0, 1))

  "AddressList form" must {

    "fail to bind when value is omitted" in {
      val expectedError = error("value", "messages__adviser__address__list__required")
      checkForError(form, emptyForm, expectedError)
    }

    "fail to bind when value is negative" in {
      val expectedError = error("value", "error.invalid", 0)
      checkForError(form, Map("value" -> "-1"), expectedError)
    }

    "fail to bind when the value is out of bounds" in {
      val expectedError = error("value", "error.invalid", 1)
      checkForError(form, Map("value" -> "2"), expectedError)
    }

  }

}