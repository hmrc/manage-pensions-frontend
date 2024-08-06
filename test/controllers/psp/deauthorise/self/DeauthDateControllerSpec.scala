/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.psp.deauthorise.self

import connectors.FakeUserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import forms.psp.deauthorise.PspDeauthDateFormProvider
import identifiers.psp.deauthorise.self.DeauthDateId
import identifiers.{AssociatedDateId, AuthorisedPractitionerId, SchemeNameId, SchemeSrnId}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testhelpers.CommonBuilders._
import uk.gov.hmrc.domain.PspId
import utils.DateHelper.formatDate
import utils.FakeNavigator
import views.html.psp.deauthorisation.self.deauthDate

import java.time.LocalDate

class DeauthDateControllerSpec extends ControllerSpecBase {

  private val formProvider = new PspDeauthDateFormProvider()
  private val date = LocalDate.parse("2020-04-01")
  private val form = formProvider(date, messages("messages__pspDeauth_date_error__before_earliest_date", formatDate(date)))

  private def onwardRoute = Call("GET", "/foo")

  private val schemeName = "test-scheme"
  private val ceaseDate = LocalDate.now()
  private val pspId = Some(PspId("00000000"))

  private val data = Json.obj(
    AssociatedDateId.toString -> date,
    SchemeNameId.toString -> schemeName,
    SchemeSrnId.toString -> srn,
    AuthorisedPractitionerId.toString -> pspDetails
  )

  private val view = injector.instanceOf[deauthDate]

  def controller(dataRetrievalAction: DataRetrievalAction = new FakeDataRetrievalAction(Some(data), pspId = pspId)) =
    new DeauthDateController(messagesApi, FakeUserAnswersCacheConnector, new FakeNavigator(onwardRoute), FakeAuthAction,
      dataRetrievalAction, new DataRequiredActionImpl, formProvider, controllerComponents, view, fakePspSchemeAuthAction)

  private def viewAsString(form: Form[_] = form) = view(form, schemeName, srn, formatDate(date))(fakeRequest, messages).toString

  "Deauth Date Controller" when {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad(srn)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "populate the view correctly on a GET if the question has previously been answered" in {

        val dataRetrieval = new FakeDataRetrievalAction(Some(data ++ Json.obj(DeauthDateId.toString -> ceaseDate)), pspId = pspId)
        val result = controller(dataRetrieval).onPageLoad(srn)(fakeRequest)
        contentAsString(result) mustBe viewAsString(form.fill(ceaseDate))
      }

      "redirect to the session expired page if there is no required data" in {
        val result = controller(getEmptyData).onPageLoad(srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onPageLoad(srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }

    "on a POST" must {
      "save the data and redirect to the next page if valid data is submitted" in {
        val postRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj(
          "pspDeauthDate.day" -> ceaseDate.getDayOfMonth.toString,
          "pspDeauthDate.month" -> ceaseDate.getMonthValue.toString,
          "pspDeauthDate.year" -> ceaseDate.getYear.toString)
        )
        val result = controller().onSubmit(srn)(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)
        FakeUserAnswersCacheConnector.verify(DeauthDateId, ceaseDate)
      }

      "return a Bad Request and errors if invalid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("pspDeauthDate", "yes"))
        val boundForm = form.bind(Map("pspDeauthDate" -> "yes"))

        val result = controller().onSubmit(srn)(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }

      "redirect to the session expired page if there is no required data" in {
        val result = controller(getEmptyData).onSubmit(srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onSubmit(srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }
  }

}
