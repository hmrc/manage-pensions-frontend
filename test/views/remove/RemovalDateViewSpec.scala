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

package views.remove

import controllers.remove._
import forms.remove.RemovalDateFormProvider
import org.joda.time.LocalDate
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import views.html.remove.removalDate

class RemovalDateViewSpec extends QuestionViewBehaviours[LocalDate] {

  private val openedDate = LocalDate.parse("2018-01-01")
  val form = new RemovalDateFormProvider()(openedDate)
  private val schemeName = "test scheme name"
  private val psaName = "test psa name"
  private val srn = "test srn"
  val prefix = "removalDate"

  private def createView: () => HtmlFormat.Appendable = () =>
    removalDate(frontendAppConfig, form, psaName, schemeName, srn)(fakeRequest, messages)

  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
    removalDate(frontendAppConfig, form, psaName, schemeName, srn)(fakeRequest, messages)

  "RemoveAsSchemeAdministrator" must {

    behave like normalPageWithTitle(createView, prefix, messages(s"messages__${prefix}__title"), messages(s"messages__${prefix}__heading", psaName, schemeName))

    behave like pageWithSubmitButton(createView)

    behave like pageWithTextFieldsWithoutErrors(createViewUsingForm, Seq("removalDate"))

    behave like pageWithReturnLink(
      createView,
      controllers.routes.SchemeDetailsController.onPageLoad(srn).url,
      messages("messages__returnToSchemeDetails__link", schemeName)
    )
  }
}
