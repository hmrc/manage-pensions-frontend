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

package views.psp

import config.FrontendAppConfig
import forms.psp.ListSchemesFormProvider
import models.{SchemeDetails, SchemeStatus}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import views.ViewSpecBase
import views.behaviours.ViewBehaviours
import views.html.psp.list_schemes

class ListSchemesViewSpec extends ViewSpecBase with ViewBehaviours with MockitoSugar with BeforeAndAfterEach {

  private val mockAppConfig = mock[FrontendAppConfig]
  override val injector = new GuiceApplicationBuilder()
    .overrides(
      Seq[GuiceableModule](
        bind[FrontendAppConfig].toInstance(mockAppConfig)
      ): _*
    ).injector()
  private val emptyList: List[SchemeDetails] = List.empty[SchemeDetails]
  private val listSchemesView = injector.instanceOf[list_schemes]
  private val listSchemesFormProvider = new ListSchemesFormProvider

  private val fullList: List[SchemeDetails] = List(

    SchemeDetails(
      "scheme-name-4",
      "reference-number-4",
      SchemeStatus.Open.value,
      Option("2017-11-09"),
      Some("PSTR-4"),
      None,
      None
    ),
    SchemeDetails(
      "scheme-name-5",
      "reference-number-5",
      SchemeStatus.Deregistered.value,
      Option("2017-11-10"),
      Some("PSTR-5"),
      None,
      None
    ),
    SchemeDetails(
      "scheme-name-6",
      "reference-number-6",
      SchemeStatus.WoundUp.value,
      Option("2017-11-11"),
      Some("PSTR-6"),
      None,
      None
    )
  )

  private val pspName = "Test psp name"

  implicit private val request: Request[_] = fakeRequest

  override def beforeEach(): Unit = {
    when(mockAppConfig.minimumSchemeSearchResults) thenReturn 5
  }

  "list-schemes view" must {

    behave like normalPageWithoutBrowserTitle(
      view = view(
        schemes = emptyList,
        numberOfSchemes = emptyList.length,
        listSchemesFormProvider.apply()
      ),
      messageKeyPrefix = "listSchemes",
      pageHeader = messages("messages__listSchemesPsp__title")
    )

    "have search bar" in {

      val doc = asDocument(view(schemes = emptyList,
        numberOfSchemes = emptyList.length,
        listSchemesFormProvider.apply()
      ).apply()
      )

      assertRenderedById(doc, "searchText-form")
    }

    "display a suitable message when there are no schemes to display against search text" in {
      view(
        schemes = emptyList,
        numberOfSchemes = emptyList.length,
        listSchemesFormProvider().bind(Map("searchText" -> "incorrect"))
      )must haveElementWithText("noSchemes", "Your search - incorrect - did not match any pension schemes.")
    }

    "display a table of schemes when there are schemes to display against search text" in {
      view(
        schemes = fullList,
        numberOfSchemes = fullList.length,
        listSchemesFormProvider().bind(Map("searchText" -> "PSTR-6"))
      ) must haveElementWithClass("scheme-list1", "dl--margin-bottom")
    }

    "display a link to return to overview page" in {
          view(
            schemes = emptyList,
            numberOfSchemes = emptyList.length,
            listSchemesFormProvider.apply()
          ) must haveLink(controllers.routes.PspDashboardController.onPageLoad().url, "return-link")
    }

  }


  private def view(schemes: List[SchemeDetails],
                   numberOfSchemes: Int,
                   form: Form[_]
                  )(implicit request: Request[_], messages: Messages): () => HtmlFormat.Appendable = () =>

    listSchemesView(
      form = form,
      schemes = schemes,
      pspName = pspName,
      numberOfSchemes = numberOfSchemes
    )
}
