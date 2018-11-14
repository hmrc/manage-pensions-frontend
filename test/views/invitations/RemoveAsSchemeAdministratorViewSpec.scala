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

import controllers.invitations._
import forms.invitations.RemoveAsSchemeAdministratorFormProvider
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.YesNoViewBehaviours
import views.html.invitations.removeAsSchemeAdministrator

class RemoveAsSchemeAdministratorViewSpec extends YesNoViewBehaviours {

  val form = new RemoveAsSchemeAdministratorFormProvider()()
  private val schemeName = "test scheme name"
  private val srn = "test srn"
  private val psaName = "test psa name"
  val prefix = "removeAsSchemeAdministrator"

  private def createView: () => HtmlFormat.Appendable = () =>
    removeAsSchemeAdministrator(frontendAppConfig, form, schemeName, srn, psaName)(fakeRequest, messages)

  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
    removeAsSchemeAdministrator(frontendAppConfig, form, schemeName, srn, psaName)(fakeRequest, messages)

  "RemoveAsSchemeAdministrator" must {

    behave like normalPageWithTitle(createView, prefix, messages(s"messages__${prefix}__title"), messages(s"messages__${prefix}__heading", psaName, schemeName))

    behave like pageWithSubmitButton(createView)

    behave like yesNoPage(createViewUsingForm, prefix, routes.RemoveAsSchemeAdministratorController.onSubmit().url)

    behave like pageWithReturnLink(
      createView,
      controllers.routes.SchemeDetailsController.onPageLoad(srn).url,
      messages("messages__returnToSchemeDetails__link", schemeName)
    )
  }
}
