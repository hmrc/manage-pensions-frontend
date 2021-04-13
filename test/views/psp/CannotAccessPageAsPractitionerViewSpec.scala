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

package views.psp

import forms.psp.CannotAccessPageAsPractitionerFormProvider
import models.AdministratorOrPractitioner
import play.api.data.Form
import views.behaviours.ViewBehaviours
import views.html.psp.cannotAccessPageAsPractitioner

class CannotAccessPageAsPractitionerViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "cannotAccessPageAsPractitioner"

  private val form = new CannotAccessPageAsPractitionerFormProvider()()

  private val cannotAccessPageAsPractitionerView = injector.instanceOf[cannotAccessPageAsPractitioner]

  private def createView() =
    () => cannotAccessPageAsPractitionerView(
      form
    )(fakeRequest, messages)

  private def createViewUsingForm =
    (form: Form[_]) => cannotAccessPageAsPractitionerView(
      form
    )(fakeRequest, messages)

  "Administrator or practitioner view" must {
    behave like normalPageWithTitle(createView(), messageKeyPrefix,
      messages(s"messages__${messageKeyPrefix}__title"), messages(s"messages__${messageKeyPrefix}__heading"))

    behave like pageWithSubmitButton(createView())

    "contain radio buttons for the value" in {
      val doc = asDocument(createViewUsingForm(form))
      for (option <- AdministratorOrPractitioner.optionsCannotAccessPageAsPractitioner) {
        assertContainsRadioButton(doc, s"value-${option.value}", "value", option.value, isChecked = false)
      }
    }

    for (option <- AdministratorOrPractitioner.optionsCannotAccessPageAsPractitioner) {
      s"rendered with a value of '${option.value}'" must {
        s"have the '${option.value}' radio button selected" in {
          val doc = asDocument(createViewUsingForm(form.bind(Map("value" -> s"${option.value}"))))
          assertContainsRadioButton(doc, s"value-${option.value}", "value", option.value, isChecked = true)

          for (unselectedOption <- AdministratorOrPractitioner.optionsCannotAccessPageAsPractitioner.filterNot(o => o == option)) {
            assertContainsRadioButton(doc, s"value-${unselectedOption.value}", "value", unselectedOption.value, isChecked = false)
          }
        }
      }
    }
  }
}
