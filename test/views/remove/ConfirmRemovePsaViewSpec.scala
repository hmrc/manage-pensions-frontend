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

package views.remove

import controllers.remove._
import forms.remove.ConfirmRemovePsaFormProvider
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.YesNoViewBehaviours
import views.html.remove.confirmRemovePsa

class ConfirmRemovePsaViewSpec extends YesNoViewBehaviours {

  val form = new ConfirmRemovePsaFormProvider()()
  private val schemeName = "test scheme name"
  private val srn = "test srn"
  private val psaName = "test psa name"
  val prefix = "confirmRemovePsa"
  private val confirmRemovePsaView = injector.instanceOf[confirmRemovePsa]

  private def createView: () => HtmlFormat.Appendable = () =>
    confirmRemovePsaView(form, schemeName, srn, psaName)(fakeRequest, messages)

  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
    confirmRemovePsaView(form, schemeName, srn, psaName)(fakeRequest, messages)

  "ConfirmRemovePsa" must {

    behave like normalPageWithTitle(createView, prefix, messages(s"messages__${prefix}__title"), messages(s"messages__${prefix}__heading", psaName, schemeName))

    behave like pageWithSubmitButton(createView)

    behave like yesNoPage(createViewUsingForm, prefix, routes.ConfirmRemovePsaController.onSubmit().url)

    behave like pageWithReturnLink(
      createView,
      controllers.routes.SchemeDetailsController.onPageLoad(srn).url,
      messages("messages__returnToSchemeDetails__link", schemeName)
    )
  }
}
