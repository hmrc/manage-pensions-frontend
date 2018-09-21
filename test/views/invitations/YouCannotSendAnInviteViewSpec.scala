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

package views.invitations

import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.invitations.youCannotSendAnInvite

class YouCannotSendAnInviteViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "youCannotSendAnInvite"

  def createView: () => HtmlFormat.Appendable = () => youCannotSendAnInvite(frontendAppConfig)(fakeRequest, messages)

  "You Cannot Send An Invite page" must {
    behave like normalPage(
      createView,
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      "_p1",
      "_p2"
    )

    "have link to return to your pension schemes" in {
      Jsoup.parse(createView().toString()).select("a[id=return-to-schemes]") must
        haveLink(controllers.routes.ListSchemesController.onPageLoad().url)
    }
  }
}
