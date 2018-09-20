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

package views.invite

import forms.invite.HaveYouEmployedPensionAdviserFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.invite.haveYouEmployedPensionAdviser

class HaveYouEmployedPensionAdviserViewSpec extends ViewBehaviours {

  val form = new HaveYouEmployedPensionAdviserFormProvider()()

  def createView: () => HtmlFormat.Appendable = () => haveYouEmployedPensionAdviser(frontendAppConfig, form, NormalMode)(fakeRequest, messages)

  def doc: Document = Jsoup.parse(createView().toString)

  val prefix = "messages_haveYouEmployedPensionAdviser__"

  "HaveYouEmployedPensionAdviser" must {

    behave like normalPageWithTitle(createView, prefix, messages(prefix + "title"), messages(prefix + "heading"))

    behave like pageWithSubmitButton(createView)

    "contain true option" in assertContainsRadioButton(doc, "value-yes", "value", "true", false)

    "contain false option" in assertContainsRadioButton(doc, "value-no", "value", "false", false)

  }

}
