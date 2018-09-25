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

package forms.invitations

import forms.mappings.AddressMapping
import javax.inject.Inject
import models.Address
import play.api.data.Form
import play.api.data.Forms.mapping
import utils.countryOptions.CountryOptions

class AdviserManualAddressFormProvider @Inject()(countryOptions: CountryOptions) extends AddressMapping {

  def apply(): Form[Address] = Form(
    mapping(
      "addressLine1" ->
        addressLineMapping("messages__error__address_line_1_required", "messages__error__address_line_1_length", "messages__error__address_line_1_invalid"),
      "addressLine2" ->
        addressLineMapping("messages__error__address_line_2_required", "messages__error__address_line_2_length", "messages__error__address_line_2_invalid"),
      "addressLine3" ->
        optionalAddressLineMapping("messages__error__address_line_3_length", "messages__error__address_line_3_invalid"),
      "addressLine4" ->
        optionalAddressLineMapping("messages__error__address_line_4_length", "messages__error__address_line_4_invalid"),
      "postCode" ->
        postCodeWithCountryMapping("messages__error__postcode", "messages__error__postcode_invalid", "messages__error__postcode_nonUK_length"),
      "country" ->
        countryMapping(countryOptions, "messages__error_country_required", "messages__error_country_invalid")
    )(Address.apply)(Address.unapply)
  )
}
