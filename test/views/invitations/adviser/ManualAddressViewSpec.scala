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

package views.invitations.adviser

import forms.invitations.adviser.ManualAddressFormProvider
import models.{Address, NormalMode}
import org.jsoup.Jsoup
import play.api.data.Form
import utils.{FakeCountryOptions, InputOption}
import views.behaviours.QuestionViewBehaviours
import views.invitations.adviser.manual_address

class ManualAddressViewSpec extends QuestionViewBehaviours[Address] {

  val messageKeyPrefix = ""
  val countryOptions: Seq[InputOption] = Seq(InputOption("AF", "Afghanistan"), InputOption("territory:AE-AZ", "Abu Dhabi"))
  val name: String = "name"

  override val form = new ManualAddressFormProvider(FakeCountryOptions())()

  def createView: () => _root_.play.twirl.api.HtmlFormat.Appendable = () =>
    manual_address(frontendAppConfig, new ManualAddressFormProvider(FakeCountryOptions()).apply())(fakeRequest, messages)

  def createViewUsingForm: (Form[_]) => _root_.play.twirl.api.HtmlFormat.Appendable = (form: Form[_]) =>
    manual_address(frontendAppConfig, form, NormalMode, name)(fakeRequest, messages)

  "ManualAddress view" must {

    behave like normalPage(createView, messageKeyPrefix, messages(s"messages__${messageKeyPrefix}__heading"))

    behave like pageWithBackLink(createView)

    behave like pageWithSecondaryHeader(createView, messages(""))

    behave like pageWithTextFields(
      createViewUsingForm,
      messageKeyPrefix,
      routes.ManualAddressController.onSubmit(NormalMode.url),"addressLine1", "addressLine2", "addressLine3", "addressLine4"
    )

    "have name rendered on the page" in {
      Jsoup.parse(createView().toString()) must haveDynamicText(name)
    }
  }

}