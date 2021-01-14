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

package views.remove

import forms.remove.RemovalDateFormProvider
import java.time.LocalDate

import org.jsoup.Jsoup
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.QuestionViewBehaviours
import utils.DateHelper._
import views.html.remove.removalDate

class RemovalDateViewSpec extends QuestionViewBehaviours[LocalDate] {

  private val associationDate = LocalDate.parse("2018-01-01")
  val form = new RemovalDateFormProvider()(associationDate, frontendAppConfig.earliestDatePsaRemoval)
  private val schemeName = "test scheme name"
  private val psaName = "test psa name"
  private val srn = "test srn"
  val prefix = "removalDate"
  private val removalDateView = injector.instanceOf[removalDate]

  private def createView: () => HtmlFormat.Appendable = () =>
    removalDateView(form, psaName, schemeName, srn, formatDate(associationDate))(fakeRequest, messages)

  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
    removalDateView(form, psaName, schemeName, srn, formatDate(associationDate))(fakeRequest, messages)

  "RemoveAsSchemeAdministrator" must {

    behave like normalPageWithTitle(createView, prefix, messages(s"messages__${prefix}__title"), messages(s"messages__${prefix}__heading", psaName, schemeName))

    behave like pageWithSubmitButton(createView)

    behave like pageWithDateFields(createViewUsingForm, form, "removalDate", "removal_date")

    behave like pageWithReturnLink(
      createView,
      controllers.routes.PsaSchemeDashboardController.onPageLoad(srn).url,
      messages("messages__returnToSchemeDetails__link", schemeName)
    )

    "display correct opening text" in {
      Jsoup.parse(createView().toString()) must haveDynamicText("messages__removalDate__lede", psaName, formatDate(associationDate))
    }
  }
}
