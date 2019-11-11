/*
 * Copyright 2019 HM Revenue & Customs
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

package views

import forms.DeleteSchemeFormProvider
import play.api.data.Form
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.YesNoViewBehaviours
import views.html.deleteScheme

class DeleteSchemeViewSpec extends YesNoViewBehaviours {

  val messageKeyPrefix = "deleteScheme"
  val schemeName = "Test Scheme Name"
  val psaName = "Test Psa Name"

  val form = new DeleteSchemeFormProvider()()

  def createView: () => HtmlFormat.Appendable = () => deleteScheme(frontendAppConfig, form, schemeName, psaName)(fakeRequest, messages)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) => deleteScheme(frontendAppConfig, form, schemeName, psaName)(fakeRequest, messages)

  "DeleteScheme view" must {

    behave like normalPageWithTitle(createView, messageKeyPrefix, Message("messages__deleteScheme__title"),
      Message("messages__deleteScheme__heading", schemeName))

    behave like pageWithBackLink(createView)

    behave like yesNoPage(createViewUsingForm, messageKeyPrefix, controllers.routes.DeleteSchemeController.onSubmit().url)

    behave like pageWithReturnLink(createView, controllers.routes.SchemesOverviewController.onPageLoad().url, messages("site.return_to", psaName))
  }
}
