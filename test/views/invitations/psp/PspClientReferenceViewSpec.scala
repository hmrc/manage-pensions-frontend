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

package views.invitations.psp

import forms.invitations.psp.PspClientReferenceFormProvider
import models.NormalMode
import models.SchemeReferenceNumber
import models.invitations.psp.ClientReference
import play.api.data.Form
import views.behaviours.ViewBehaviours
import views.html.invitations.psp.pspClientReference

class PspClientReferenceViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "psp_client_ref"
  private val pspName = "PSP Name"

  private val schemeName = "Test Scheme"

  private val returnCall = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber("srn"))

  val formProvider = new PspClientReferenceFormProvider
  val form: Form[ClientReference] = formProvider()

  private val pspClientRefView = injector.instanceOf[pspClientReference]

  private def createView() =
    () => pspClientRefView(form, pspName, NormalMode, schemeName, returnCall)(fakeRequest, messages)

  private def createViewUsingForm =
    (form: Form[_]) => pspClientRefView(form, pspName, NormalMode, schemeName, returnCall)(fakeRequest, messages)

  "PspClientReferenceView" must {
    behave like normalPageWithTitle(createView(), messageKeyPrefix, messages(s"messages__${messageKeyPrefix}__title"),
      messages(s"messages__${messageKeyPrefix}__heading", pspName))

    behave like pageWithSubmitButton(createView())

    "contain radio buttons for the value" in {
      val doc = asDocument(createViewUsingForm(form))
      for (option <- ClientReference.options) {
        assertContainsRadioButton(doc, s"value_hasReference-${option.value}", "value.hasReference", option.value, isChecked = false)
      }
    }

    for (option <- ClientReference.options) {
      s"rendered with a value of '${option.value}'" must {
        s"have the '${option.value}' radio button selected" in {
          val doc = asDocument(createViewUsingForm(form.bind(Map("value.hasReference" -> s"${option.value}"))))
          assertContainsRadioButton(doc, s"value_hasReference-${option.value}", "value.hasReference", option.value, isChecked = true)

          for (unselectedOption <- ClientReference.options.filterNot(o => o == option)) {
            assertContainsRadioButton(doc, s"value_hasReference-${unselectedOption.value}", "value.hasReference", unselectedOption.value, isChecked = false)
          }
        }
      }
    }
  }
}

