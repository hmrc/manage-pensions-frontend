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

import connectors.admin.MinimalConnector
import connectors.{EmailSent, EmailConnector, PspConnector}
import controllers.ControllerSpecBase
import controllers.actions._
import forms.remove.RemovePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.invitations.psp.PspNameId
import identifiers.remove.pspSelfRemoval.RemovalDateId
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{MinimalPSAPSP, SendEmailRequest}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, when, verify}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.http.HttpResponse
import views.html.remove.pspSelfRemoval.declaration

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar {

  private val formProvider = new RemovePspDeclarationFormProvider()
  private val form = formProvider()
  private val mockPspConnector: PspConnector = mock[PspConnector]
  private val mockEmailConnector = mock[EmailConnector]
  private val mockMinimalConnector = mock[MinimalConnector]

  private def onwardRoute = controllers.remove.pspSelfRemoval.routes.ConfirmationController.onPageLoad()
  private val schemeName = "test-scheme"
  private val srn = "srn"
  private val pstr = "pstr"
  private val pspId = Some(PspId("00000000"))

  private val pspName: String = "psp-name"
  private val minPsp: MinimalPSAPSP =
    MinimalPSAPSP("z@z.z", isPsaSuspended = false, Some("ABC Corps"), None, rlsFlag = false, deceasedFlag = false)

  private val data = Json.obj(
    PSTRId.toString -> pstr,
    SchemeNameId.toString -> schemeName,
    SchemeSrnId.toString -> srn,
    RemovalDateId.toString -> "2020-12-12",
    PspNameId.toString -> pspName
  )

  private val view = injector.instanceOf[declaration]

  def controller(dataRetrievalAction: DataRetrievalAction = new FakeDataRetrievalAction(Some(data), pspId = pspId)): DeclarationController =
    new DeclarationController(messagesApi, formProvider, FakeAuthAction, dataRetrievalAction,
      new DataRequiredActionImpl, mockPspConnector, crypto, frontendAppConfig,
      mockMinimalConnector, mockEmailConnector, controllerComponents, view)

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
      "save the data and redirect to the next page if valid data is submitted and also send an email to the PSP" in {
        when(mockPspConnector.deAuthorise(any(), any())(any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.obj("processingDate" -> LocalDate.now)))))
        when(mockMinimalConnector.getMinimalPspDetails(any())(any(), any()))
          .thenReturn(Future.successful(minPsp))
        when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(EmailSent))
        val postRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj("value" -> true))
        val result = controller().onSubmit()(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)

        val emailRequestCaptor = ArgumentCaptor.forClass(classOf[SendEmailRequest])
        verify(mockEmailConnector, times(1)).sendEmail(emailRequestCaptor.capture())(any(), any())
        val actualSendEmailRequest = emailRequestCaptor.getValue

        actualSendEmailRequest.to mustBe List(minPsp.email)
        actualSendEmailRequest.templateId mustBe "pods_psp_de_auth_psp_company_partnership"
        actualSendEmailRequest.parameters mustBe Map(
          "authorisingPsaName" -> "psa name",
          "pspName" -> pspName,
          "schemeName" -> schemeName
        )

        //actualSendEmailRequest.eventUrl.isDefined mustBe true
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
