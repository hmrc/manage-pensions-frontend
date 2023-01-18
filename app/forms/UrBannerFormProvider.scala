/*
 * Copyright 2023 HM Revenue & Customs
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

package forms

import forms.mappings.{EmailMapping, Mappings, Transforms}
import models.URBanner
import play.api.data.Form
import play.api.data.Forms._

import javax.inject.Inject

class UrBannerFormProvider @Inject() extends Mappings with Transforms with EmailMapping {

  private val regexPersonOrOrganisationName =   """^[a-zA-Z\u00C0-\u00FF '??\u2014\u2013\u2010\u002d]{1,107}"""

  def apply(): Form[URBanner] =
    Form(
      mapping(
        "indOrgName" ->
          text(errorKey = "").verifying(
            regexp(regexPersonOrOrganisationName, "messages__banner__error" )
          ),
        "email" -> emailMapping()
      )(URBanner.apply)(URBanner.unapply)
    )
}


