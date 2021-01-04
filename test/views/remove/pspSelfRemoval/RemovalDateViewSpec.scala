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

package views.remove.pspSelfRemoval

import java.time.LocalDate

import forms.remove.PspRemovalDateFormProvider
import org.jsoup.Jsoup
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.DateHelper._
import views.behaviours.QuestionViewBehaviours
import views.html.remove.pspSelfRemoval.removalDate

class RemovalDateViewSpec extends QuestionViewBehaviours[LocalDate] {

  private val relationshipStartDate = LocalDate.parse("2020-04-01")
  val form = new PspRemovalDateFormProvider()(relationshipStartDate, "messages__pspRemoval_date_error__before_relationshipStartDate")
  private val schemeName = "test scheme name"
  private val srn = "test srn"
  val prefix = "pspSelfRemovalDate"
  private val removalDateView = injector.instanceOf[removalDate]

  private def createView: () => HtmlFormat.Appendable = () =>
    removalDateView(form, schemeName, srn, formatDate(relationshipStartDate))(fakeRequest, messages)

  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
    removalDateView(form, schemeName, srn, formatDate(relationshipStartDate))(fakeRequest, messages)

  "RemovalDate" must {

    behave like normalPageWithTitle(
      createView,
      prefix,
      messages(s"messages__${prefix}__title"),
      messages(s"messages__${prefix}__heading", schemeName)
    )

    behave like pageWithSubmitButton(createView)

    behave like pageWithDateFields(
      view = createViewUsingForm,
      form = form,
      idkey = "pspRemovalDate",
      msgKey = "removal_date"
    )

    behave like pageWithReturnLink(
      view = createView,
      url = controllers.routes.PspSchemeDashboardController.onPageLoad(srn).url,
      text = messages("messages__returnToSchemeDetails__link", schemeName)
    )

    "display correct opening text" in {
      Jsoup.parse(createView().toString()) must haveDynamicText(
        "messages__pspSelfRemovalDate__lede",
        formatDate(relationshipStartDate)
      )
    }
  }
}
