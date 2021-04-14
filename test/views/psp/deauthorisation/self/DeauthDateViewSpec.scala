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

package views.psp.deauthorisation.self

import controllers.psp.routes._
import forms.psp.deauthorise.PspDeauthDateFormProvider
import java.time.LocalDate
import org.jsoup.Jsoup
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.DateHelper._
import views.behaviours.QuestionViewBehaviours
import views.html.psp.deauthorisation.self.deauthDate


class DeauthDateViewSpec extends QuestionViewBehaviours[LocalDate] {

  private val relationshipStartDate = LocalDate.parse("2020-04-01")
  val form = new PspDeauthDateFormProvider()(relationshipStartDate, "messages__pspDeauth_date_error__before_relationshipStartDate")
  private val schemeName = "test scheme name"
  private val srn = "test srn"
  val prefix = "pspSelfDeauthDate"
  private val deauthDateView = injector.instanceOf[deauthDate]

  private def createView: () => HtmlFormat.Appendable = () =>
    deauthDateView(form, schemeName, srn, formatDate(relationshipStartDate))(fakeRequest, messages)

  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
    deauthDateView(form, schemeName, srn, formatDate(relationshipStartDate))(fakeRequest, messages)

  "DeauthDate" must {

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
      idkey = "pspDeauthDate",
      msgKey = "Deauth_date"
    )

    behave like pageWithReturnLink(
      view = createView,
      url = PspSchemeDashboardController.onPageLoad(srn).url,
      text = messages("messages__returnToSchemeDetails__link", schemeName)
    )

    "display correct opening text" in {
      Jsoup.parse(createView().toString()) must haveDynamicText(
        "messages__pspSelfDeauthDate__lede",
        formatDate(relationshipStartDate)
      )
    }
  }
}
