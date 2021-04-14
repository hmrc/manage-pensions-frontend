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

package views.psp.deauthorisation

import controllers.psa.routes._
import forms.psp.deauthorise.PspDeauthDateFormProvider
import org.jsoup.Jsoup
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.DateHelper._
import views.behaviours.QuestionViewBehaviours
import views.html.psp.deauthorisation.pspDeauthDate

import java.time.LocalDate

class PspDeauthDateViewSpec extends QuestionViewBehaviours[LocalDate] {

  private val relationshipStartDate = LocalDate.parse("2020-04-01")
  val form = new PspDeauthDateFormProvider()(relationshipStartDate, "messages__pspDeauth_date_error__before_relationshipStartDate")
  private val schemeName = "test scheme name"
  private val pspName = "test psp name"
  private val srn = "test srn"
  val prefix = "pspDeauthDate"
  private val deauthDateView = injector.instanceOf[pspDeauthDate]

  private def createView: () => HtmlFormat.Appendable = () =>
    deauthDateView(form, pspName, schemeName, srn, formatDate(relationshipStartDate), 0)(fakeRequest, messages)

  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
    deauthDateView(form, pspName, schemeName, srn, formatDate(relationshipStartDate), 0)(fakeRequest, messages)

  "PspDeauthDate" must {

    behave like normalPageWithTitle(
      createView,
      prefix,
      messages(s"messages__${prefix}__title"),
      messages(s"messages__${prefix}__heading", pspName, schemeName)
    )

    behave like pageWithSubmitButton(createView)

    behave like pageWithDateFields(
      view = createViewUsingForm,
      form = form,
      idkey = "pspDeauthDate",
      msgKey = "deauth_date"
    )

    behave like pageWithReturnLink(
      view = createView,
      url = PsaSchemeDashboardController.onPageLoad(srn).url,
      text = messages("messages__returnToSchemeDetails__link", schemeName)
    )

    "display correct opening text" in {
      Jsoup.parse(createView().toString()) must haveDynamicText(
        "messages__pspDeauthDate__lede",
        pspName,
        formatDate(relationshipStartDate)
      )
    }
  }
}
