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

package views.invitations

import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.invitations.incorrectPsaDetails

class incorrectPsaDetailsViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "incorrectPsaDetails"
  val invitee = "PSA"
  val srn = "test-srn"
  val schemeName = "test-scheme-name"

  def createView(): () => HtmlFormat.Appendable = () => incorrectPsaDetails(frontendAppConfig, invitee, srn, schemeName)(fakeRequest, messages)

  "IncorrectPsaDetails view" must {

    behave like normalPage(createView(), messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__title"),
      "_text2"
    )

    "display PSA name dynamically" in {
      Jsoup.parse(createView()().toString()) must haveDynamicText(s"messages__${messageKeyPrefix}__text1", invitee)
    }

    "include a link to the scheme details page" in {
      createView() must haveLink(controllers.routes.SchemeDetailsController.onPageLoad(srn).url, "return-link")
    }

    "include dynamic text for the scheme details page" in {
      createView() must haveElementWithText("return-text", messages("messages__incorrectPsaDetails__linkText", schemeName))
    }

  }

}
