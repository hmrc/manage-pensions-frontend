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

import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.invitations.invitation_duplicate

class InvitationDuplicateViewSpec extends ViewBehaviours {

  private val testInviteeName: String = "Joe Bloggs"
  private val testSchemeName: String = "Test Scheme Ltd"
  private val messageKeyPrefix = "invitationDuplicate"

  private val view = injector.instanceOf[invitation_duplicate]

  def createView(): () => HtmlFormat.Appendable = () =>
    view(
      testInviteeName,
      testSchemeName
    )(fakeRequest, messages)

  "Invitation Success Page" must {

    behave like normalPage(
      createView(),
      messageKeyPrefix,
      Message("messages__invitationDuplicate__heading", testInviteeName)
    )

    "state the scheme and invitee names" in {
      createView() must haveElementWithText("schemeName", Message("messages__invitationDuplicate__schemeName", testInviteeName, testSchemeName))
    }

    "state invite information text" in {
      createView() must haveElementWithText("inviteInformation", Message("messages__invitationDuplicate__inviteInformation"))
    }

    "must have link to list schemes page" in {
      createView() must haveElementWithText("return-to-schemes", Message("messages__invitationDuplicate__returnToSchemes__link"))
    }
  }
}

