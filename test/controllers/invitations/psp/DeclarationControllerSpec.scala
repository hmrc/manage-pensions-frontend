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

package controllers.invitations.psp

import audit.{AuditService, PSPAuthorisationAuditEvent, PSPAuthorisationEmailAuditEvent}
import base.JsonFileReader
import connectors.admin.MinimalConnector
import connectors.scheme.ListOfSchemesConnector
import connectors.{ActiveRelationshipExistsException, EmailConnector, EmailSent, PspConnector}
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.invitations.psp.DeclarationFormProvider
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.{BeforeAndAfterEach, RecoverMethods}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers._
import services.SchemeDetailsService
import utils.UserAnswers
import views.html.invitations.psp.declaration

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach with JsonFileReader
  with RecoverMethods {

  def onwardRoute: Call = controllers.invitations.psp.routes.ConfirmationController.onPageLoad()

  val formProvider = new DeclarationFormProvider()
  val form: Form[Boolean] = formProvider()

  val config: Configuration = injector.instanceOf[Configuration]
  private val view = injector.instanceOf[declaration]

  private def sessionExpired: String = controllers.routes.SessionExpiredController.onPageLoad.url

  private val mockPspConnector = mock[PspConnector]
  private val mockListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val mockSchemeDetailsService = mock[SchemeDetailsService]
  private val mockEmailConnector = mock[EmailConnector]
  private val mockMinimalConnector = mock[MinimalConnector]
  private val mockAuditService = mock[AuditService]

  val srn: String = "srn"
  val pstr: String = "pstr"
  val pspName: String = "psp-name"
  val schemeName: String = "scheme-name"
  val pspId: String = "psp-id"
  val pspCR: String = "psp-client-reference"
  val minPsa: MinimalPSAPSP = MinimalPSAPSP("z@z.z", isPsaSuspended = false, Some("ABC Corps"), None, rlsFlag = false, deceasedFlag = false)

  val userAnswers: UserAnswers = UserAnswers().set(SchemeSrnId)(srn).asOpt.value
    .set(SchemeNameId)(schemeName).asOpt.value
    .set(PspNameId)(pspName).asOpt.value
    .set(PspId)(pspId).asOpt.value
    .set(PspClientReferenceId)(pspCR).asOpt.value

  val data: DataRetrievalAction = new FakeDataRetrievalAction(Some(userAnswers.json))
  val listOfSchemesResponse: Future[Right[Nothing, ListOfSchemes]] = Future.successful(Right(ListOfSchemes("", "", None)))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditService)
    doNothing().when(mockAuditService).sendEvent(any())(any(), any())
  }

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) = new DeclarationController(
    messagesApi,
    formProvider,
    FakeAuthAction,
    dataRetrievalAction,
    new DataRequiredActionImpl,
    mockPspConnector,
    mockListOfSchemesConnector,
    mockSchemeDetailsService,
    mockEmailConnector,
    mockMinimalConnector,
    controllerComponents,
    mockAuditService,
    crypto,
    frontendAppConfig,
    view,
    fakePspSchemeAuthAction
  )

  private def viewAsString(form: Form[_] = form) = view(form)(fakeRequest, messages).toString

  "Declaration Controller" when {

    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad()(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }
    }

    "on a POST" must {

      "invite psp, redirect to confirmation page when valid data is submitted and send an email and email audit event" in {
        when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(listOfSchemesResponse)
        when(mockSchemeDetailsService.pstr(any(), any())).thenReturn(Some(pstr))
        when(mockPspConnector.authorisePsp(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(()))
        when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(EmailSent))
        when(mockMinimalConnector.getMinimalPsaDetails(any())(any(), any()))
          .thenReturn(Future.successful(minPsa))

        val result = controller(data).onSubmit()(fakeRequest.withFormUrlEncodedBody("declaration" -> "true"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url

        val emailRequestCaptor = ArgumentCaptor.forClass(classOf[SendEmailRequest])

        verify(mockEmailConnector, times(1)).sendEmail(emailRequestCaptor.capture())(any(), any())
        val actualSendEmailRequest = emailRequestCaptor.getValue

        actualSendEmailRequest.to mustBe List(minPsa.email)
        actualSendEmailRequest.templateId mustBe "pods_authorise_psp"
        actualSendEmailRequest.parameters mustBe Map(
          "psaInvitor" -> minPsa.name,
          "pspInvitee" -> pspName,
          "schemeName" -> schemeName
        )
        actualSendEmailRequest.eventUrl.isDefined mustBe true

        val expectedEmailAuditEvent = PSPAuthorisationEmailAuditEvent(
          psaId = "A0000000",
          pspId = pspId,
          pstr = pstr,
          emailAddress = minPsa.email,
          Sent
        )
        verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedEmailAuditEvent))(any(), any())

        val expectedAuditEvent = PSPAuthorisationAuditEvent(
          psaId = "A0000000",
          pspId = pspId,
          pstr = pstr
        )
        verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())

      }

      "return Bad Request if invalid data is submitted" in {
        val formWithErrors = form.withError("declaration", messages("messages__error__psp_declaration__required"))
        val result = controller().onSubmit()(fakeRequest)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(formWithErrors)
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onSubmit()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }

      "redirect to already associated page if already associated" in {
        when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(listOfSchemesResponse)
        when(mockSchemeDetailsService.pstr(any(), any())).thenReturn(Some(pstr))
        when(mockPspConnector.authorisePsp(any(), any(), any(), any())(any(), any())).thenReturn(Future.failed(new ActiveRelationshipExistsException))
        when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(EmailSent))
        when(mockMinimalConnector.getMinimalPsaDetails(any())(any(), any()))
          .thenReturn(Future.successful(minPsa))

        val result = controller(data).onSubmit()(fakeRequest.withFormUrlEncodedBody("declaration" -> "true"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.invitations.psp.routes.AlreadyAssociatedWithSchemeController.onPageLoad.url
      }

    }
  }
}
