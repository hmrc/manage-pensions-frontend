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

package views.remove

import base.SpecBase
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.remove.confirmRemoved

class ConfirmRemovedViewSpec extends ViewBehaviours {

  import ConfirmRemovedViewSpec._

  "Confirm Removed Page" must {

    behave like normalPage(
      createView(this),
      messageKeyPrefix,
      Message("messages__confirmRemoved__heading", testPsaName, testSchemeName)
    )

    behave like pageWithReturnLink(createView(this), returnLinkUrl, messages("messages__confirmRemoved__return_link"))

  }

}

object ConfirmRemovedViewSpec {

  val messageKeyPrefix = "confirmRemoved"
  val testPsaName = "test-pas-name"
  val testSchemeName = "test-scheme-name"

  lazy val returnLinkUrl: String = controllers.routes.ListSchemesController.onPageLoad().url

  def createView(base: SpecBase): () => HtmlFormat.Appendable = () =>
    confirmRemoved(
      base.frontendAppConfig,
      testPsaName,
      testSchemeName
    )(base.fakeRequest, base.messages)

}
