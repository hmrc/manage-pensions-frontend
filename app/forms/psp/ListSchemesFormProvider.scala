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

package forms.psp

import forms.mappings.Mappings
import javax.inject.Inject
import forms.mappings.Transforms
import play.api.data.Form

class ListSchemesFormProvider @Inject()() extends Mappings with Transforms {

  def apply(): Form[String] = Form(
    "searchText" -> text("messages__listSchemesPsp__search_required").
    transform(standardTextTransform, noTransform).
      verifying(firstError(
        search("messages__listSchemesPsp__search_invalid")))
  )
}


