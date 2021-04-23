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

package views.psa

import controllers.psa.routes._
import controllers.psa.remove.routes._
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import viewmodels.AssociatedPsa
import views.ViewSpecBase
import views.behaviours.ViewBehaviours
import views.html.psa.viewAdministrators

class ViewAdministratorsViewSpec extends ViewSpecBase with ViewBehaviours {
  private val messageKeyPrefix = "psaSchemeDash"
  private val schemeName = "Test Scheme Name"
  private val administrators = Seq(AssociatedPsa("First Psa", canRemove = true), AssociatedPsa("Second User", canRemove = false))
  private val administratorsNoRemove = Seq(AssociatedPsa("First Psa", canRemove = false), AssociatedPsa("Second User", canRemove = false))
  private val srn = "P12345678"
  private val viewAdministratorsView = injector.instanceOf[viewAdministrators]

  def createView(psaList: Option[Seq[AssociatedPsa]] = Some(administrators),
                 isSchemeOpen: Boolean = true): () => HtmlFormat.Appendable =
    () => viewAdministratorsView(schemeName, psaList, srn, isSchemeOpen)(fakeRequest, messages)

  "ViewAdministrators view" must {
    behave like normalPageWithoutBrowserTitle(
      createView(),
      messageKeyPrefix,
      messages("messages__psaSchemeDash__psa_list_head"),
      "_invite_link"
    )

    "contain list of administrators" in {
      for (psa <- administrators) Jsoup.parse(createView()().toString) must haveDynamicText(psa.name)
    }

    "render the 'Remove' link if a PSA can be removed from the scheme" in {
      appRunning()
      createView() must haveLink(RemovePsaController.onPageLoad().url, "remove-link")
    }

    "not render the 'Remove' link if no PSAs can be removed from the scheme" in {
      createView(psaList = Some(administratorsNoRemove)) must notHaveElementWithId("remove-link")
    }

    "not contain list of administrators if not data is returned from API" in {
      for (psa <- administrators)
        Jsoup.parse(createView(psaList = None)().toString) mustNot haveDynamicText(psa.name)
    }

    "have link to Invite another PSA" in {
      Jsoup.parse(createView()().toString()).select("a[id=invite]") must
        haveLink(controllers.invitations.routes.InviteController.onPageLoad(srn).url)

    }

    "not have link to Invite another PSA" when {
      "scheme status is not open" in {
        Jsoup.parse(createView(isSchemeOpen = false)().toString()).select("a[id=invite]") mustNot
          haveLink(PsaSchemeDashboardController.onPageLoad(srn).url)
      }
    }

    "have link to return to list of schemes page" in {
      Jsoup.parse(createView()().toString()).select("a[id=return]") must
        haveLink(PsaSchemeDashboardController.onPageLoad(srn).url)
    }

  }
}