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

package views.invitations

import forms.invitations.AdviserAddressPostcodeLookupFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.StringViewBehaviours
import views.html.invitations.adviserPostcode

class AdviserPostCodeLookupViewSpec extends StringViewBehaviours {

  val messageKeyPrefix = "adviser__postcode"

  val form = new AdviserAddressPostcodeLookupFormProvider()()

  val adviserName = "test adviser"

  def createView: () => HtmlFormat.Appendable = () => adviserPostcode(frontendAppConfig, form, adviserName)(fakeRequest, messages)
  def createViewUsingForm: Form[String] => HtmlFormat.Appendable = (form: Form[String]) => adviserPostcode(frontendAppConfig, form,
    adviserName)(fakeRequest, messages)

  "Address view" must {
    behave like normalPage(createView, messageKeyPrefix, messages(s"messages__${messageKeyPrefix}__heading", adviserName), "_lede")

    behave like pageWithBackLink(createView)

    behave like stringPage(createViewUsingForm, messageKeyPrefix, controllers.invitations.routes.AdviserAddressPostcodeLookupController.onSubmit().url,
      Some("messages__common__address_postcode"))

    "have link for enter address manually" in {
      Jsoup.parse(createView().toString()).select("a[id=manual-address-link]") must haveLink(
        controllers.invitations.routes.AdviserManualAddressController.onPageLoad(NormalMode, false).url)
    }

    behave like pageWithSubmitButton(createView)
  }
}
