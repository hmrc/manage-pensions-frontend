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

package controllers.psp.deauthorise

import audit.{AuditService, PSPDeauthorisationByPSAAuditEvent, PSPDeauthorisationByPSAEmailAuditEvent}
import connectors._
import connectors.admin.MinimalConnector
import controllers.actions.{AuthAction, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.psp.deauthorise.DeauthorisePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.psp.deauthorise
import identifiers.{SchemeNameId, SchemeSrnId, SeqAuthorisedPractitionerId}
import models.{IndividualDetails, MinimalPSAPSP, SchemeReferenceNumber, SendEmailRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsJson}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, _}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import views.html.psp.deauthorisation.psaDeauthorisePspDeclaration

import java.time.LocalDate
import scala.concurrent.Future

class PsaDeauthPspDeclarationControllerSpec
  extends ControllerWithQuestionPageBehaviours
    with MockitoSugar
    with BeforeAndAfterEach {

  import PsaDeauthPspDeclarationControllerSpec._

  private val formProvider: DeauthorisePspDeclarationFormProvider = new DeauthorisePspDeclarationFormProvider()
  private val form = formProvider

  private val view = app.injector.instanceOf[psaDeauthorisePspDeclaration]

  private val mockPspConnector: PspConnector = mock[PspConnector]

  private val mockEmailConnector = mock[EmailConnector]

  private val mockMinimalConnector = mock[MinimalConnector]

  private val mockAuditService = mock[AuditService]

  def controller(
                  dataRetrievalAction: DataRetrievalAction = sessionData,
                  fakeAuth: AuthAction = FakeAuthAction,
                  userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) =
    new PsaDeauthPspDeclarationController(
      messagesApi = messagesApi,
      userAnswersCacheConnector = userAnswersCacheConnector,
      navigator = navigator,
      authenticate = fakeAuth,
      getData = dataRetrievalAction,
      requireData = requiredDataAction,
      pspConnector = mockPspConnector,
      formProvider = formProvider,
      controllerComponents = controllerComponents,
      auditService = mockAuditService,
      minimalConnector = mockMinimalConnector,
      appConfig = frontendAppConfig,
      emailConnector = mockEmailConnector,
      crypto,
      view = view,
      fakePsaSchemeAuthAction
    )

  override def beforeEach(): Unit = {
    reset(mockPspConnector)
    reset(mockEmailConnector)
    reset(mockMinimalConnector)
    reset(mockAuditService)
    when(mockPspConnector.deAuthorise(any(), any())(any(), any())).thenReturn(
      Future.successful(HttpResponse.apply(OK, Json.stringify(Json.obj("processingDate" -> LocalDate.now))))
    )
    when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(EmailSent))
    when(mockMinimalConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minPsa))
    when(mockAuditService.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
  }

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onPageLoad(0, srn)
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onSubmit(0, srn)
  }

  private def onSaveAction(userAnswersConnector: UserAnswersCacheConnector): Action[AnyContent] = {
    controller(userAnswersCacheConnector = userAnswersConnector).onSubmit(0, srn)
  }

  private def viewAsString(form: Form[Boolean]): String =
    view(form, schemeName, srn, 0)(fakeRequest, messages).toString

  private def viewAsStringPostRequest(form: Form[Boolean]): String =
    view(form, schemeName, srn, 0)(postRequest, messages).toString

  behave like controllerWithOnPageLoadMethodWithoutPrePopulation(
    onPageLoadAction = onPageLoadAction,
    emptyData = sessionData,
    emptyForm = form(),
    validView = viewAsString
  )

  behave like controllerWithOnSubmitMethod(
    onSubmitAction = onSubmitAction,
    validData = validData,
    form = form().bind(Map("value" -> "")),
    errorView = viewAsStringPostRequest,
    postRequest = postRequest,
    emptyPostRequest = Some(emptyPostRequest)
  )

  "send a deauthorise practitioner audit event when psp successfully deauthorised by the PSA" in {
    val result = onSubmitAction(validData, FakeAuthAction)(postRequest)

    when(mockMinimalConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minPsa))

    status(result) mustBe SEE_OTHER

    redirectLocation(result) mustBe Some(onwardRoute.url)

    // scalastyle:off magic.number
    val expectedAuditEvent = PSPDeauthorisationByPSAAuditEvent(LocalDate.of(2020, 5, 1), "A0000000", "A2200005", pstr)

    verify(mockAuditService, times(1)).sendExtendedEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
  }

  "send an email to the PSA email address and send an email audit event when psp successfully deauthorised by the PSA" in {
    val result = onSubmitAction(validData, FakeAuthAction)(postRequest)

    when(mockMinimalConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minPsa))

    status(result) mustBe SEE_OTHER

    redirectLocation(result) mustBe Some(onwardRoute.url)

    val emailRequestCaptor = ArgumentCaptor.forClass(classOf[SendEmailRequest])
    verify(mockEmailConnector, times(1)).sendEmail(emailRequestCaptor.capture())(any(), any())
    val actualSendEmailRequest = emailRequestCaptor.getValue

    actualSendEmailRequest.to mustBe List(minPsa.email)
    actualSendEmailRequest.templateId mustBe frontendAppConfig.emailPsaDeauthorisePspTemplateId
    actualSendEmailRequest.parameters mustBe Map(
      "psaName" -> minPsa.name,
      "pspName" -> "PSP Limited Company 1",
      "schemeName" -> schemeName
    )
    actualSendEmailRequest.eventUrl.isDefined mustBe true

    val expectedAuditEvent = PSPDeauthorisationByPSAEmailAuditEvent(
      psaId = "A0000000",
      pspId = "A2200005",
      pstr = pstr,
      emailAddress = minPsa.email
    )
    verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
  }

  behave like controllerThatSavesUserAnswers(
    saveAction = onSaveAction,
    validRequest = postRequest,
    id = deauthorise.PsaDeauthorisePspDeclarationId(0),
    value = true
  )
}

object PsaDeauthPspDeclarationControllerSpec {
  private val schemeName = "test scheme name"
  val srn: SchemeReferenceNumber = SchemeReferenceNumber("AB123456C")
  private val pstr = "pstr"
  private val practitioners = JsArray(
    Seq(
      Json.obj(
        "authorisingPSAID" -> "A0000000",
        "authorisingPSA" -> Json.obj(
          "firstName" -> "Nigel",
          "lastName" -> "Smith",
          "middleName" -> "Robert"
        ),
        "relationshipStartDate" -> "2020-04-01",
        "id" -> "A2200005",
        "organisationOrPartnershipName" -> "PSP Limited Company 1"
      ),
      Json.obj(
        "authorisingPSAID" -> "A2100007",
        "authorisingPSA" -> Json.obj(
          "organisationOrPartnershipName" -> "Acme Ltd"
        ),
        "relationshipStartDate" -> "2020-04-01",
        "id" -> "A2200007",
        "individual" -> Json.obj(
          "firstName" -> "PSP Individual",
          "lastName" -> "Second"
        )
      )
    )
  )

  private val individualEmail = "individual@ind@com"

  private val individualFirstName = "Joe"
  private val individualLastName = "Bloggs"

  private val minPsa = MinimalPSAPSP(
    email = individualEmail,
    isPsaSuspended = false,
    organisationName = None,
    individualDetails = Some(IndividualDetails(firstName = individualFirstName, middleName = None, lastName = individualLastName)),
    rlsFlag = false,
    deceasedFlag = false
  )

  private val data = Json.obj(
    SchemeSrnId.toString -> srn,
    SchemeNameId.toString -> schemeName,
    SeqAuthorisedPractitionerId.toString -> practitioners,
    PSTRId.toString -> pstr,
    deauthorise.PspDeauthDateId(0).toString -> "2020-05-01"
  )

  private val sessionData: FakeDataRetrievalAction =
    new FakeDataRetrievalAction(Some(data))

  private val validData: FakeDataRetrievalAction =
    new FakeDataRetrievalAction(Some(
      data
        ++
        Json.obj(deauthorise.PsaDeauthorisePspDeclarationId(0).toString -> "true")
    ))

  val postRequest: FakeRequest[AnyContentAsJson] =
    FakeRequest().withJsonBody(Json.obj(
      "declaration" -> "true"
    ))

  val emptyPostRequest: FakeRequest[AnyContentAsJson] =
    FakeRequest().withJsonBody(Json.obj(
      "declaration" -> ""
    ))
}
