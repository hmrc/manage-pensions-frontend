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

package views.invitations

import controllers.invitations.routes._
import forms.invitations.AdviserManualAddressFormProvider
import models.Address
import models.NormalMode
import org.jsoup.Jsoup
import play.api.data.Form
import utils.FakeCountryOptions
import views.behaviours.QuestionViewBehaviours
import views.html.invitations.adviserAddress

class AdviserManualAddressViewSpec extends QuestionViewBehaviours[Address] {

  val messageKeyPrefix = "adviser__address"
  val name: String = "Pension Adviser"

  val countryOptions = new FakeCountryOptions(environment, frontendAppConfig)

  override val form = new AdviserManualAddressFormProvider(countryOptions)()

  private val adviserAddressView = injector.instanceOf[adviserAddress]

  def createView: () => _root_.play.twirl.api.HtmlFormat.Appendable = () =>
    adviserAddressView(
      new AdviserManualAddressFormProvider(countryOptions)(),
      NormalMode,
      countryOptions.options,
      false,
      messageKeyPrefix,
      name
    )(fakeRequest, messages)

  def createViewUsingForm: Form[_] => _root_.play.twirl.api.HtmlFormat.Appendable = (form: Form[_]) =>
    adviserAddressView(form, NormalMode, countryOptions.options, false, messageKeyPrefix, name)(fakeRequest, messages)

  "ManualAddress view" must {

    behave like normalPage(createView, messageKeyPrefix, messages(s"messages__${messageKeyPrefix}__heading", name))

    behave like pageWithBackLink(createView)

    behave like pageWithSecondaryHeader(createView, messages(""))

    behave like pageWithErrorOutsideLabel(
      createViewUsingForm,
      messageKeyPrefix,
      AdviserManualAddressController.onSubmit(NormalMode, false).url,
      "addressLine1", "addressLine2", "addressLine3", "addressLine4"
    )

    "have name rendered on the page" in {
      Jsoup.parse(createView().toString()) must haveDynamicText(name)
    }
  }

}
