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
import views.html.triage.invitingPSTRStartWithTwo

class InvitingPSTRStartWithTwoViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "invitingPSTRStartWithTwo"
  private val managePensionSchemesServiceLink = s"${frontendAppConfig.loginUrl}?continue=${frontendAppConfig.loginToListSchemesUrl}"
  private val view = injector.instanceOf[invitingPSTRStartWithTwo]

  def createView: () => HtmlFormat.Appendable = () =>
    view()(fakeRequest, messages)

  "InvitingPSTRStartWithTwoView" must {
    behave like normalPageWithTitle(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__title"),
      messages(s"messages__${messageKeyPrefix}__title"),
      "_p1", "_p2", "_p3"
    )

    "have button link to redirect to list schemes url" in {
      Jsoup.parse(createView().toString()).select("a[id=submit]") must
        haveLink(managePensionSchemesServiceLink)
    }
  }
}
