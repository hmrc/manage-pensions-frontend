/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.DeleteSchemeChangesFormProvider
import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.YesNoViewBehaviours
import views.html.deleteSchemeChanges

class DeleteSchemeChangesViewSpec extends YesNoViewBehaviours {

  val messageKeyPrefix = "deleteSchemeChanges"
  val schemeName = "Test Scheme Name"
  val psaName = "Test psa name"
  val srn = "S123"
  val postCall: Call = controllers.routes.DeleteSchemeChangesController.onSubmit(srn)
  val form = new DeleteSchemeChangesFormProvider()()
  val view = injector.instanceOf[deleteSchemeChanges]

  def createView: () => HtmlFormat.Appendable = () => view(form, schemeName, postCall, psaName)(fakeRequest, messages)

  def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) => view(form, schemeName, postCall, psaName)(fakeRequest, messages)

  "DeleteScheme view" must {

    behave like normalPageWithTitle(createView, messageKeyPrefix, Message("messages__deleteSchemeChanges__title"),
      Message("messages__deleteSchemeChanges__heading", schemeName), "_p1", "_p2")

    behave like pageWithBackLink(createView)

    behave like yesNoPage(createViewUsingForm, messageKeyPrefix, controllers.routes.DeleteSchemeChangesController.onSubmit(srn).url)
  }
}
