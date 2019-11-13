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

import java.time.LocalDate

import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import utils.DateHelper
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.invitations.invitation_success

class InvitationSuccessSpec extends ViewBehaviours {
  private val testSrn = "test-srn"
  private val testInviteeName: String = "Joe Bloggs"
  private val testSchemeName: String = "Test Scheme Ltd"
  private val testExpiryDate: LocalDate = LocalDate.now()

  private val messageKeyPrefix = "invitationSuccess"

  private val continue: Call = controllers.invitations.routes.InvitationSuccessController.onSubmit(testSrn)

  private val view = injector.instanceOf[invitation_success]

  def createView(): () => HtmlFormat.Appendable = () =>
    view(
      testInviteeName,
      testSchemeName,
      testExpiryDate,
      continue
    )(fakeRequest, messages)

  "Invitation Success Page" must {

    behave like normalPage(
      createView(),
      messageKeyPrefix,
      Message("messages__invitationSuccess__heading", testInviteeName)
    )

    "state the scheme name" in {
      createView() must haveElementWithText("schemeName", Message("messages__invitationSuccess__schemeName", testSchemeName))
    }

    "state invitee will be send an email" in {
      createView() must haveElementWithText("emailAdvice", Message("messages__invitationSuccess__emailAdvice", testInviteeName))
    }

    "state expiry date of invitation" in {
      createView() must haveElementWithText("expiryDate", Message("messages__invitationSuccess__expiryDate", DateHelper.formatDate(testExpiryDate)))
    }

    behave like pageWithSubmitButton(createView())
  }
}
