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

package views.invitations

import forms.invitations.PensionAdviserAddressListFormProvider
import models.{NormalMode, TolerantAddress}
import play.api.data.Form
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.invitations.pension_adviser_address_list

class PensionAdviserAddressListViewSpec extends ViewBehaviours {

  private val messageKeyPrefix = "adviser__address__list"

  private val form = new PensionAdviserAddressListFormProvider()(Nil)

  private val addresses = Seq(address("postcode 1"), address("postcode 2"))
  private val addressIndexes = Seq.range(0, 2)
  private def call = controllers.invitations.routes.AdviserManualAddressController.onPageLoad(NormalMode, false)

  private val view = injector.instanceOf[pension_adviser_address_list]

  def address(postCode: String): TolerantAddress = TolerantAddress(
    Some("address line 1"),
    Some("address line 2"),
    Some("test town"),
    Some("test county"),
    Some(postCode),
    Some("United Kingdom")
  )

  private def createView: () => HtmlFormat.Appendable =
    () =>
      view(
        form,
        addresses,
        NormalMode
      )(fakeRequest, messages)

  private def createViewUsingForm: Form[_] => HtmlFormat.Appendable =
    (form: Form[_]) =>
      view(
        form,
        addresses,
        NormalMode
      )(fakeRequest, messages)

  "AddressListView view" must {
    behave like normalPage(createView, messageKeyPrefix, messages("messages__adviser__address__list__heading"))

    behave like pageWithBackLink(createView)

    "have link for enter address manually" in {
      createView must haveLink(call.url, "manual-address-link")
    }
  }

  "AddressListView view" when {

    "rendered" must {
      "contain radio buttons for the value" in {
        val doc = asDocument(createViewUsingForm(form))
        for (i <- addressIndexes) {
          assertContainsRadioButton(doc, s"value-$i", "value", s"$i", isChecked = false)
        }
      }
    }

    for (index <- addressIndexes) {
      s"rendered with a value of '$index'" must {
        s"have the '$index' radio button selected" in {
          val doc = asDocument(createViewUsingForm(form.bind(Map("value" -> s"$index"))))
          assertContainsRadioButton(doc, s"value-$index", "value", s"$index", isChecked = true)

          for (unselectedIndex <- addressIndexes.filterNot(o => o == index)) {
            assertContainsRadioButton(doc, s"value-$unselectedIndex", "value", unselectedIndex.toString, isChecked = false)
          }
        }
      }
    }

  }

}