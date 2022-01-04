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

package forms.behaviours

import forms.mappings.AddressMapping
import org.scalatest.Matchers
import org.scalatest.WordSpec
import play.api.data.Form
import play.api.data.FormError
import wolfendale.scalacheck.regexp.RegexpGen

trait AddressBehaviours extends WordSpec with Matchers with FieldBehaviours {

  def formWithPostCode(form: Form[_], fieldName: String, keyRequired: String, keyLength: String, keyInvalid: String): Unit = {

    "behave like a form with a Post Code" should {

      behave like fieldThatBindsValidData(
        form,
        fieldName,
        RegexpGen.from(AddressMapping.postCodeRegex.toString)
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        maxLength = AddressMapping.maxPostCodeLength,
        lengthError = FormError(fieldName, keyLength, Seq(AddressMapping.maxPostCodeLength))
      )

      behave like mandatoryField(
        form,
        fieldName,
        requiredError = FormError(fieldName, keyRequired)
      )

      behave like fieldWithRegex(
        form,
        fieldName,
        "12AB AB1",
        FormError(fieldName, keyInvalid, Seq(AddressMapping.postCodeRegex.toString))
      )

      "transform the Post Code value correctly" in {
        val postCode = "  zz11zz  "
        val result = form.bind(Map(fieldName -> postCode))
        result.errors.size shouldBe 0
        result.get shouldBe "ZZ1 1ZZ"
      }
    }

  }

}
