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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package views.invitations
//
//import forms.invitations.psa.DoYouHaveWorkingKnowledgeFormProvider
//import models.NormalMode
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Document
//import play.twirl.api.HtmlFormat
//import views.behaviours.ViewBehaviours
//import views.html.invitations.doYouHaveWorkingKnowledge
//
//class DoYouHaveWorkingKnowledgeViewSpec extends ViewBehaviours {
//
//  val form = new DoYouHaveWorkingKnowledgeFormProvider()()
//
//  private val doYouHaveWorkingKnowledgeView = injector.instanceOf[doYouHaveWorkingKnowledge]
//
//  def createView: () => HtmlFormat.Appendable = () => doYouHaveWorkingKnowledgeView(form, NormalMode)(fakeRequest, messages)
//
//  def doc: Document = Jsoup.parse(createView().toString)
//
//  val prefix = "doYouHaveWorkingKnowledge"
//
//  "HaveYouEmployedPensionAdviser" must {
//
//    behave like normalPageWithTitle(createView, prefix, messages(s"messages__${prefix}__title"), messages(s"messages__${prefix}__heading"), "p1")
//
//    behave like pageWithBackLink(createView)
//
//    behave like pageWithSubmitButton(createView)
//
//    "contain true option" in assertContainsRadioButton(doc, "value-yes", "value", "true", false)
//
//    "contain false option" in assertContainsRadioButton(doc, "value-no", "value", "false", false)
//
//  }
//
//}
