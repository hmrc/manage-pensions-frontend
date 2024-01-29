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

package forms.mappings

import play.api.data.Form
import play.api.data.Mapping
import utils.FakeCountryOptions
import views.behaviours.AddressBehaviours

class AddressMappingSpec extends AddressBehaviours {

  private val fieldName = "addressLine"
  private val keyAddressRequired = "error.address_line.required"
  private val keyAddressLength = "error.address_line.length"
  private val keyAddressInvalid = "error.address_line.invalid"

  "Address mapping" should {

    val mapping: Mapping[String] = addressLineMapping(keyAddressRequired, keyAddressLength, keyAddressInvalid)
    val form: Form[String] = Form(fieldName -> mapping)

    behave like formWithAddressField(
      form,
      fieldName,
      keyAddressRequired,
      keyAddressLength,
      keyAddressInvalid
    )

  }

  "Optional address mapping" should {

    val mapping: Mapping[Option[String]] = optionalAddressLineMapping(keyAddressLength, keyAddressInvalid)
    val form: Form[Option[String]] = Form(fieldName -> mapping)

    behave like formWithOptionalAddressField(
      form,
      fieldName,
      keyAddressLength,
      keyAddressInvalid,
      Map(fieldName -> "test-address-line"),
      (optionalField: Option[String]) => optionalField
    )

  }

  "Post Code with Country mapping" should {

    val form: Form[Option[String]] = Form("postCode" -> postCodeWithCountryMapping("error.required", "error.invalid", "error.postcode.nonUK.length"))

    behave like formWithCountryAndPostCode(
      form,
      "error.required",
      "error.invalid",
      "error.postcode.nonUK.length",
      Map.empty[String, String],
      (s: Option[String]) => s.getOrElse("")
    )

  }

  "Post Code mapping" should {

    val keyRequired = "error.required"
    val keyLength = "error.length"
    val keyInvalid = "error.invalid"

    val form = Form("postCode" -> postCodeMapping(keyRequired, keyLength, keyInvalid))

    behave like formWithPostCode(
      form,
      "postCode",
      keyRequired,
      keyLength,
      keyInvalid
    )

  }

  "Country mapping" should {

    val keyRequired = "error.required"
    val keyInvalid = "error.invalid"

    val countryOptions = new FakeCountryOptions(environment, frontendAppConfig)

    val form = Form("country" -> countryMapping(countryOptions, keyRequired, keyInvalid))

    behave like formWithCountry(form, "country", keyRequired, keyInvalid, countryOptions, Map.empty)

  }

  "postCodeTransform" must {
    "strip leading and trailing spaces" in {
      val actual = postCodeTransform(" AB12 1AB ")
      actual shouldBe "AB12 1AB"
    }

    "upper case all characters" in {
      val actual = postCodeTransform("ab12 1ab")
      actual shouldBe "AB12 1AB"
    }

    "minimise spaces" in {
      val actual = postCodeTransform("AB12     1AB")
      actual shouldBe "AB12 1AB"
    }
  }

  "postCodeValidTransform" must {
    "add missing internal space in full post code" in {
      val actual = postCodeValidTransform("AB121AB")
      actual shouldBe "AB12 1AB"
    }

    "add missing internal space in minimal post code" in {
      val actual = postCodeValidTransform("A11AB")
      actual shouldBe "A1 1AB"
    }
  }

}
