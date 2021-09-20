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
//package views.psp.deauthorisation
//
//import controllers.psa.routes._
//import controllers.psp.deauthorise.routes._
//import forms.psp.deauthorise.ConfirmDeauthPspFormProvider
//import models.Index
//import play.api.data.Form
//import play.twirl.api.HtmlFormat
//import views.behaviours.YesNoViewBehaviours
//import views.html.psp.deauthorisation.confirmDeauthorisePsp
//
//class ConfirmDeauthorisePspViewSpec extends YesNoViewBehaviours {
//
//  val form = new ConfirmDeauthPspFormProvider()()
//  private val schemeName = "test scheme name"
//  private val srn = "test srn"
//  private val pspName = "test psa name"
//  val prefix = "confirmDeauthorisePsp"
//  private val confirmDeauthorisePspView = injector.instanceOf[confirmDeauthorisePsp]
//
//  private def createView: () => HtmlFormat.Appendable = () =>
//    confirmDeauthorisePspView(form, schemeName, srn, pspName, Index(0))(fakeRequest, messages)
//
//  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable = (form: Form[_]) =>
//    confirmDeauthorisePspView(form, schemeName, srn, pspName, Index(0))(fakeRequest, messages)
//
//  "ConfirmDeauthorisePsp" must {
//
//    behave like normalPageWithTitle(
//      view = createView,
//      messageKeyPrefix = prefix,
//      title = messages(s"messages__${prefix}__title"),
//      pageHeader =
//        messages(s"messages__${prefix}__heading", pspName, schemeName) +
//          messages(s"messages__${prefix}__heading__screenReaderAlternativeText", pspName, schemeName)
//    )
//
//    behave like pageWithSubmitButton(createView)
//
//    behave like yesNoPage(
//      createView = createViewUsingForm,
//      messageKeyPrefix = prefix,
//      expectedFormAction = ConfirmDeauthorisePspController.onSubmit(Index(0)).url
//    )
//
//    behave like pageWithReturnLink(
//      view = createView,
//      url = PsaSchemeDashboardController.onPageLoad(srn).url,
//      text = messages("messages__returnToSchemeDetails__link", schemeName)
//    )
//  }
//}
