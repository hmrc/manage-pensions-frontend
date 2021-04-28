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

package views.psp.deauthorisation

import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.psp.deauthorisation.confirmPsaDeauthPsp

class ConfirmPsaDeauthPspViewSpec extends ViewBehaviours {
  val messageKeyPrefix = "confirmPsaDeauthPsp"
  private val pspName = "PSP Limited Company 1"
  private val schemeName = "test-scheme-name"
  private val psaEmail = "a@b.com"
  private val view = injector.instanceOf[confirmPsaDeauthPsp]

  lazy val returnLinkUrl: String = controllers.psa.routes.ListSchemesController.onPageLoad().url

  def createView(): () => HtmlFormat.Appendable = () =>
    view(
      pspName = pspName,
      schemeName = schemeName,
      psaEmailAddress = psaEmail
    )(fakeRequest, messages)

  "Confirm Deauthorised Page" must {

    behave like normalPage(
      view = createView(),
      messageKeyPrefix = messageKeyPrefix,
      pageHeader =
        Message("messages__confirmPsaDeauthPsp__heading", pspName, schemeName).resolve + " " +
          Message("messages__confirmPsaDeauthPsp__heading__screenReaderAlternative", pspName, schemeName).resolve
    )

    behave like pageWithReturnLink(
      view = createView(),
      url = returnLinkUrl,
      text = messages("messages__confirmDeauth__return_link")
    )

  }
}