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

import base.SpecBase
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.invitations.invitation_duplicate

class InvitationDuplicateSpec extends ViewBehaviours {

  import InvitationDuplicateSpec._

  "Invitation Success Page" must {

    behave like normalPage(
      createView(this),
      messageKeyPrefix,
      Message("messages__invitationDuplicate__heading", testInviteeName)
    )

    "state the scheme name" in {
      createView(this) must haveElementWithText("schemeName", Message("messages__invitationDuplicate__schemeName", testSchemeName))
    }
  }
}

object InvitationDuplicateSpec {

  val testInviteeName: String = "Joe Bloggs"
  val testSchemeName: String = "Test Scheme Ltd"
  val messageKeyPrefix = "invitationDuplicate"


  def createView(base: SpecBase): () => HtmlFormat.Appendable = () =>
    invitation_duplicate(
      base.frontendAppConfig,
      testInviteeName,
      testSchemeName
    )(base.fakeRequest, base.messages)
}
