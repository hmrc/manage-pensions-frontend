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

package views.triage

import forms.triage.WhatDoYouWantToDoFormProvider
import models.triage.WhatDoYouWantToDo
import play.api.data.Form
import views.behaviours.ViewBehaviours
import views.html.triage.whatDoYouWantToDo

class WhatDoYouWantToDoViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "whatDoYouWantToDo"

  private val form = new WhatDoYouWantToDoFormProvider()()

  private val whatDoYouWantToDoView = injector.instanceOf[whatDoYouWantToDo]

  private def createView() =
    () => whatDoYouWantToDoView(
      form
    )(fakeRequest, messages)

  private def createViewUsingForm =
    (form: Form[_]) => whatDoYouWantToDoView(
      form
    )(fakeRequest, messages)

  "DoesPSTRStartWithTwoView" must {
    behave like normalPage(createView(), messageKeyPrefix, messages(s"messages__${messageKeyPrefix}__title"))

    behave like pageWithSubmitButton(createView())

    "contain radio buttons for the value" in {
      val doc = asDocument(createViewUsingForm(form))
      for (option <- WhatDoYouWantToDo.options) {
        assertContainsRadioButton(doc, s"value-${option.value}", "value", option.value, isChecked = false)
      }
    }

    for (option <- WhatDoYouWantToDo.options) {
      s"rendered with a value of '${option.value}'" must {
        s"have the '${option.value}' radio button selected" in {
          val doc = asDocument(createViewUsingForm(form.bind(Map("value" -> s"${option.value}"))))
          assertContainsRadioButton(doc, s"value-${option.value}", "value", option.value, isChecked = true)

          for (unselectedOption <- WhatDoYouWantToDo.options.filterNot(o => o == option)) {
            assertContainsRadioButton(doc, s"value-${unselectedOption.value}", "value", unselectedOption.value, isChecked = false)
          }
        }
      }
    }
  }
}

