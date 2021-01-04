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

package controllers.remove.pspSelfRemoval

import java.time.LocalDate

import connectors.PspConnector
import controllers.ControllerSpecBase
import controllers.actions._
import forms.remove.RemovePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.{SchemeNameId, SchemeSrnId}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.remove.pspSelfRemoval.declaration

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar {

  private val formProvider = new RemovePspDeclarationFormProvider()
  private val form = formProvider()
  private val mockPspConnector: PspConnector = mock[PspConnector]

  private def onwardRoute = controllers.remove.pspSelfRemoval.routes.ConfirmationController.onPageLoad()
  private val schemeName = "test-scheme"
  private val srn = "srn"
  private val pstr = "pstr"
  private val pspId = Some(PspId("00000000"))

  private val data = Json.obj(
    PSTRId.toString -> pstr,
    SchemeNameId.toString -> schemeName,
    SchemeSrnId.toString -> srn
  )

  private val view = injector.instanceOf[declaration]

  def controller(dataRetrievalAction: DataRetrievalAction = new FakeDataRetrievalAction(Some(data), pspId = pspId)): DeclarationController =
    new DeclarationController(messagesApi, formProvider, FakeAuthAction, dataRetrievalAction,
      new DataRequiredActionImpl, mockPspConnector, stubMessagesControllerComponents(), view)

  private def viewAsString(form: Form[Boolean] = form) = view(form, schemeName, srn)(fakeRequest, messages).toString

  "Declaration Controller" when {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad()(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to the session expired page if there is no required data" in {
        val result = controller(getEmptyData).onPageLoad()(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
    }

    "on a POST" must {
      "save the data and redirect to the next page if valid data is submitted" in {
        when(mockPspConnector.deAuthorise(any(), any())(any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.obj("processingDate" -> LocalDate.now)))))
        val postRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj("value" -> true))
        val result = controller().onSubmit()(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)
      }

      "return a Bad Request and errors if invalid data is submitted" in {
        val postRequest = fakeRequest.withFormUrlEncodedBody(("value", ""))
        val boundForm = form.bind(Map("value" -> ""))

        val result = controller().onSubmit()(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }

      "redirect to the session expired page if there is no required data" in {
        val result = controller(getEmptyData).onSubmit()(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onSubmit()(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
    }
  }

}
