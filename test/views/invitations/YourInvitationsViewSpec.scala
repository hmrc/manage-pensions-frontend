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

package views.invitations

import models.invitations.psp.Invitation
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import testhelpers.InvitationBuilder._
import utils.DateHelper._
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.invitations.yourInvitations

class YourInvitationsViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "yourInvitations"
  val psaName = "Test psa name"
  private val yourInvitationsView = injector.instanceOf[yourInvitations]

  def createView(invitations: List[Invitation] = invitationList): () => HtmlFormat.Appendable = () =>
    yourInvitationsView(invitations, psaName)(fakeRequest, messages)

  "Your Invitations view" must {

    behave like normalPage(
      createView(),
      messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__heading"),
      s"_lede"
    )

    behave like pageWithBackLink(createView())

    "display no invitations content if there are no invitations" in {
      assertContainsText(Jsoup.parse(createView(invitations = Nil)().toString()), Message("messages__yourInvitations__not_found"))
    }

    "display details for all invitations" in {
      Jsoup.parse(createView()().toString) must haveDynamicText(invitation1.schemeName)
      Jsoup.parse(createView()().toString) must
        haveDynamicText(Message("messages__yourInvitations__scheme_expiry_date", displayExpiryDate(invitation1.expireAt.toLocalDate)))
      Jsoup.parse(createView()().toString).select("a[id=accept-invitation-0]") must
        haveLink(controllers.invitations.routes.YourInvitationsController.onSelect(srn).url)

      Jsoup.parse(createView()().toString) must haveDynamicText(invitation2.schemeName)
      Jsoup.parse(createView()().toString) must
        haveDynamicText(Message("messages__yourInvitations__scheme_expiry_date", displayExpiryDate(invitation2.expireAt.toLocalDate)))
      Jsoup.parse(createView()().toString).select("a[id=accept-invitation-1]") must
        haveLink(controllers.invitations.routes.YourInvitationsController.onSelect(srn).url)
    }

  }
}
