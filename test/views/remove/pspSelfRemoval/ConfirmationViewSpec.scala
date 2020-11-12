/*
 * Copyright 2020 HM Revenue & Customs
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

package views.remove.pspSelfRemoval

import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.remove.pspSelfRemoval.confirmation

class ConfirmationViewSpec extends ViewBehaviours {
  val messageKeyPrefix = "pspRemovalConfirmation"
  private val psaName = "PSA Limited Company 1"
  private val schemeName = "test-scheme-name"
  private val email = "a@b.com"
  private val view = injector.instanceOf[confirmation]

  lazy val returnLinkUrl: String = controllers.routes.PspDashboardController.onPageLoad().url

  def createView(): () => HtmlFormat.Appendable = () =>
    view(schemeName, psaName, email)(fakeRequest, messages)

  "Confirm Removed Page" must {

    behave like normalPage(
      view = createView(),
      messageKeyPrefix = messageKeyPrefix,
      pageHeader =
        Message("messages__pspRemovalConfirmation__heading", schemeName).resolve + " " +
          Message("messages__pspRemovalConfirmation__heading__screenReaderAlternative", schemeName).resolve
    )

    behave like pageWithReturnLink(
      view = createView(),
      url = returnLinkUrl,
      text = messages("site.return_to_psp_overview")
    )

  }
}
