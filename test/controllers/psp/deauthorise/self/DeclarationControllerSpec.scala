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

package controllers.psp.deauthorise.self

import audit.{AuditService, PSPSelfDeauthorisationEmailAuditEvent}
import connectors.admin.MinimalConnector
import connectors.{EmailConnector, EmailSent, PspConnector}
import controllers.ControllerSpecBase
import controllers.actions._
import forms.psp.deauthorise.DeauthorisePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.psp.deauthorise.self.DeauthDateId
import identifiers.{AuthorisedPractitionerId, SchemeNameId, SchemeSrnId}
import models.{IndividualDetails, MinimalPSAPSP, SendEmailRequest, Sent}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testhelpers.CommonBuilders.pspDetails
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.http.HttpResponse
import views.html.psp.deauthorisation.self.declaration

import java.time.LocalDate
import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  private val formProvider = new DeauthorisePspDeclarationFormProvider()
  private val form = formProvider()
  private val mockPspConnector: PspConnector = mock[PspConnector]
  private val mockEmailConnector = mock[EmailConnector]
  private val mockMinimalConnector = mock[MinimalConnector]
  private val mockAuditService = mock[AuditService]

  private def onwardRoute = controllers.psp.deauthorise.self.routes.ConfirmationController.onPageLoad()
  private val schemeName = "test-scheme"
  private val srn = "srn"
  private val pstr = "pstr"
  private val pspId = "00000000"
  private val optionalPspId = Some(PspId(pspId))
  private val emailAddress = "z@z.z"
  private val expectedPspSelfDeauthorisationEmailAuditEvent = PSPSelfDeauthorisationEmailAuditEvent(pspId, pstr, emailAddress, Sent)

  private val minPspOrganisation: MinimalPSAPSP =
    MinimalPSAPSP("z@z.z", isPsaSuspended = false,
      Some("psp-name-org"), None, rlsFlag = false, deceasedFlag = false)
  private val minPspIndividual: MinimalPSAPSP =
    MinimalPSAPSP("z@z.z", isPsaSuspended = false, None,
      Some(IndividualDetails("Test", None, "Psp Name")), rlsFlag = false, deceasedFlag = false)

  private val data = Json.obj(
    PSTRId.toString -> pstr,
    SchemeNameId.toString -> schemeName,
    SchemeSrnId.toString -> srn,
    DeauthDateId.toString -> "2020-12-12",
    AuthorisedPractitionerId.toString -> pspDetails
  )

  private val view = injector.instanceOf[declaration]

  def controller(dataRetrievalAction: DataRetrievalAction = new FakeDataRetrievalAction(Some(data), pspId = optionalPspId)): DeclarationController =
    new DeclarationController(messagesApi, formProvider, FakeAuthAction, dataRetrievalAction,
      new DataRequiredActionImpl, mockPspConnector,
      mockMinimalConnector, mockEmailConnector, mockAuditService,
      crypto,
      appConfig = frontendAppConfig,
      controllerComponents, view
    )

  private def viewAsString(form: Form[Boolean] = form) = view(form, schemeName, srn)(fakeRequest, messages).toString

  override def beforeEach(): Unit = {
    reset(mockPspConnector, mockMinimalConnector, mockEmailConnector, mockAuditService)

  }

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
      "save data, redirect to next page if valid data is submitted, send email to PSP using correct template for a company and send splunk audit event" in {
        when(mockPspConnector.deAuthorise(any(), any())(any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.obj("processingDate" -> LocalDate.now)))))
        when(mockMinimalConnector.getMinimalPspDetails(any())(any(), any()))
          .thenReturn(Future.successful(minPspOrganisation))
        when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(EmailSent))

        val emailAuditEventCaptor = ArgumentCaptor.forClass(classOf[PSPSelfDeauthorisationEmailAuditEvent])
        doNothing().when(mockAuditService).sendEvent(emailAuditEventCaptor.capture())(any(), any())

        val postRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj("value" -> true))
        val result = controller().onSubmit()(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)

        val emailRequestCaptor = ArgumentCaptor.forClass(classOf[SendEmailRequest])
        verify(mockEmailConnector, times(1)).sendEmail(emailRequestCaptor.capture())(any(), any())
        val actualSendEmailRequest = emailRequestCaptor.getValue

        actualSendEmailRequest.to mustBe List(minPspOrganisation.email)
        actualSendEmailRequest.templateId mustBe "pods_psp_de_auth_psp_company_partnership"
        actualSendEmailRequest.parameters mustBe Map(
          "authorisingPsaName" -> pspDetails.authorisingPSA.name,
          "pspName" -> minPspOrganisation.name,
          "schemeName" -> schemeName
        )

        actualSendEmailRequest.eventUrl.isDefined mustBe true
        emailAuditEventCaptor.getValue mustBe expectedPspSelfDeauthorisationEmailAuditEvent
      }

      "save data, redirect to next page if valid data is submitted, send email to PSP using correct template for an individual and send splunk audit event" in {
        when(mockPspConnector.deAuthorise(any(), any())(any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.obj("processingDate" -> LocalDate.now)))))
        when(mockMinimalConnector.getMinimalPspDetails(any())(any(), any()))
          .thenReturn(Future.successful(minPspIndividual))
        when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(EmailSent))
        val postRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj("value" -> true))
        val result = controller().onSubmit()(postRequest)

        val emailAuditEventCaptor = ArgumentCaptor.forClass(classOf[PSPSelfDeauthorisationEmailAuditEvent])
        verify(mockAuditService, times(1)).sendEvent(emailAuditEventCaptor.capture())(any(), any())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)

        val emailRequestCaptor = ArgumentCaptor.forClass(classOf[SendEmailRequest])
        verify(mockEmailConnector, times(1)).sendEmail(emailRequestCaptor.capture())(any(), any())
        val actualSendEmailRequest = emailRequestCaptor.getValue

        actualSendEmailRequest.to mustBe List(minPspOrganisation.email)
        actualSendEmailRequest.templateId mustBe "pods_psp_de_auth_psp_individual"
        actualSendEmailRequest.parameters mustBe Map(
          "authorisingPsaName" -> pspDetails.authorisingPSA.name,
          "pspName" -> minPspIndividual.name,
          "schemeName" -> schemeName
        )

        actualSendEmailRequest.eventUrl.isDefined mustBe true
        emailAuditEventCaptor.getValue mustBe expectedPspSelfDeauthorisationEmailAuditEvent
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
