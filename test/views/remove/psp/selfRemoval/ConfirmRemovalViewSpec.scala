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

package views.remove.psp.selfRemoval

import controllers.psp.routes._
import controllers.psp.deauthorise.self._
import forms.remove.psa.ConfirmRemovePsaFormProvider
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.YesNoViewBehaviours
import views.html.remove.psp.selfRemoval.confirmRemoval

class ConfirmRemovalViewSpec extends YesNoViewBehaviours {

  val form = new ConfirmRemovePsaFormProvider()()
  private val schemeName = "test scheme name"
  private val srn = "test srn"
  private val pspName = "test psp name"
  val prefix = "confirmRemovePsp"
  private val confirmRemovalView = injector.instanceOf[confirmRemoval]

  private def createView: () => HtmlFormat.Appendable = () =>
    confirmRemovalView(form, schemeName, srn, pspName)(fakeRequest, messages)

  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
    confirmRemovalView(form, schemeName, srn, pspName)(fakeRequest, messages)

  "ConfirmRemoval" must {

    behave like normalPageWithTitle(
      view = createView,
      messageKeyPrefix = prefix,
      title = messages(s"messages__${prefix}__title"),
      pageHeader = messages(s"messages__${prefix}__heading", pspName, schemeName) +
        messages(s"messages__${prefix}__heading__screenReaderAlternativeText", pspName, schemeName)
    )

    behave like pageWithSubmitButton(createView)

    behave like yesNoPage(
      createView = createViewUsingForm,
      messageKeyPrefix = prefix,
      expectedFormAction = routes.ConfirmRemovalController.onSubmit().url
    )

    behave like pageWithReturnLink(
      view = createView,
      url = PspSchemeDashboardController.onPageLoad(srn).url,
      text = messages("messages__returnToSchemeDetails__link", schemeName)
    )
  }
}
