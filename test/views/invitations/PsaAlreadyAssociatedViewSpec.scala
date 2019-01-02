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

import base.SpecBase
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.invitations.{invitation_duplicate, psa_already_associated}

class PsaAlreadyAssociatedViewSpec extends ViewBehaviours {

  import PsaAlreadyAssociatedViewSpec._

  "Psa already associated Page" must {

    behave like normalPage(
      createView(this),
      messageKeyPrefix,
      Message("messages__psaAlreadyAssociated__heading", testInviteeName)
    )

    "state the scheme and invitee names" in {
      createView(this) must haveElementWithText("schemeName", Message("messages__psaAlreadyAssociated__schemeName", testInviteeName, testSchemeName))
    }

    "state invite information text" in {
      createView(this) must haveElementWithText("inviteInformation", Message("messages__psaAlreadyAssociated__inviteInformation"))
    }

    "must have link to list schemes page" in {
      createView(this) must haveElementWithText("return-to-schemes", Message("messages__psaAlreadyAssociated__returnToSchemes__link"))
    }
  }
}

object PsaAlreadyAssociatedViewSpec {

  val testInviteeName: String = "Joe Bloggs"
  val testSchemeName: String = "Test Scheme Ltd"
  val messageKeyPrefix = "psaAlreadyAssociated"


  def createView(base: SpecBase): () => HtmlFormat.Appendable = () =>
    psa_already_associated(
      base.frontendAppConfig,
      testInviteeName,
      testSchemeName
    )(base.fakeRequest, base.messages)
}
