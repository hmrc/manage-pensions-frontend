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

import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours
import views.html.invitations.declaration

class DeclarationViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "declaration"

  def declarationView(hasAdviser: Boolean = true, isMasterTrust: Boolean = false): () => HtmlFormat.Appendable = () =>
    declaration(frontendAppConfig, hasAdviser, isMasterTrust)(fakeRequest, messages)

  behave like normalPageWithTitle(
    declarationView(),
    messageKeyPrefix,
    Message("messages__declaration__title"),
    Message("messages__declaration__heading"),
    "_continue",
    "_statement1",
    "_statement2",
    "_statement3",
    "_statement4",
    "_statement6"
  )

  "declaration view" must {
    "have fit and proper declaration if no adviser" in {
      val document = asDocument(declarationView(hasAdviser = false)())
      document must haveDynamicText("messages__declaration__statement5__no__adviser")
      document must not (haveDynamicText("messages__declaration__statement5__with__adviser"))
    }

    "have no working knowledge declaration if have an adviser" in {
      val document = asDocument(declarationView()())
      document must haveDynamicText("messages__declaration__statement5__with__adviser")
      document must not (haveDynamicText("messages__declaration__statement5__no__adviser"))
    }

    "have master trust declaration if master trust" in {
      val document = asDocument(declarationView(isMasterTrust = true)())
      document must haveDynamicText("messages__declaration__statement7")
    }

    "not have master trust declaration if not master trust" in {
      val document = asDocument(declarationView()())
      document must not (haveDynamicText("messages__declaration__statement7"))
    }
  }

}
