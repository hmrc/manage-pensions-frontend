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

package views.triage

import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.triage.aTwoAndAZeroIds

class ATwoAndAZeroIdsViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "aTwoAndAZeroIds"
  private val managePensionSchemesServiceLink = s"${frontendAppConfig.loginUrl}?continue=${frontendAppConfig.registeredPsaDetailsUrl}"
  private val pensionSchemesOnlineServiceLink = frontendAppConfig.tpssInitialQuestionsUrl
  private val returnLink = frontendAppConfig.guidanceStartPageGovUkLink
  private val view = injector.instanceOf[aTwoAndAZeroIds]

  def createView: () => HtmlFormat.Appendable = () =>
    view(managePensionSchemesServiceLink, pensionSchemesOnlineServiceLink, returnLink)(fakeRequest, messages)

  "Psa Suspended page" must {
    behave like normalPageWithTitle(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__title"),
      messages(s"messages__${messageKeyPrefix}__h1")
    )

    "have link to redirect to change psa details url" in {
      Jsoup.parse(createView().toString()).select("a[id=manage-pension-schemes-service]") must
        haveLink(managePensionSchemesServiceLink)
    }

    "have link to redirect to tpss" in {
      Jsoup.parse(createView().toString()).select("a[id=pension-schemes-online-service]") must
        haveLink(pensionSchemesOnlineServiceLink)
    }

    "have link to return to guidance start page" in {
      Jsoup.parse(createView().toString()).select("a[id=return-to-guidance]") must
        haveLink(returnLink)
    }
  }
}
