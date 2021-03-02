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

package controllers.remove

import java.time.LocalDate
import audit.{AuditService, PSPDeauthorisationEmailAuditEvent, PSPDeauthorisationAuditEvent}
import connectors.EmailConnector
import connectors.EmailSent
import connectors.PspConnector
import controllers.actions.AuthAction
import controllers.actions.DataRetrievalAction
import controllers.actions.FakeAuthAction
import controllers.actions.FakeDataRetrievalAction
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.remove.RemovePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.remove.{PspRemovalDateId, PsaRemovePspDeclarationId}
import identifiers.SchemeNameId
import identifiers.SchemeSrnId
import identifiers.SeqAuthorisedPractitionerId
import org.mockito.Matchers.any
import org.mockito.Mockito.reset
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers.redirectLocation
import play.api.test.Helpers.status
import uk.gov.hmrc.http.HttpResponse
import views.html.remove.psaRemovePspDeclaration
import connectors.FakeUserAnswersCacheConnector
import connectors.UserAnswersCacheConnector
import connectors.admin.MinimalConnector
import models.IndividualDetails
import models.MinimalPSAPSP
import models.SendEmailRequest
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class PsaRemovePspDeclarationControllerSpec extends ControllerWithQuestionPageBehaviours with MockitoSugar with BeforeAndAfterEach {

  import PsaRemovePspDeclarationControllerSpec._

  private val formProvider: RemovePspDeclarationFormProvider = new RemovePspDeclarationFormProvider()
  private val form = formProvider

  private val view = app.injector.instanceOf[psaRemovePspDeclaration]

  private val mockPspConnector: PspConnector = mock[PspConnector]

  private val mockEmailConnector = mock[EmailConnector]

  private val mockMinimalConnector = mock[MinimalConnector]

  private val mockAuditService = mock[AuditService]

  def controller(
                  dataRetrievalAction: DataRetrievalAction = sessionData,
                  fakeAuth: AuthAction = FakeAuthAction,
                  userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) =
    new PsaRemovePspDeclarationController(
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
      view = view
    )

  override def beforeEach(): Unit = {
    reset(mockPspConnector, mockEmailConnector, mockMinimalConnector, mockAuditService)
    when(mockPspConnector.deAuthorise(any(), any())(any(), any())).thenReturn(
      Future.successful(HttpResponse.apply(OK, Json.stringify(Json.obj("processingDate" -> LocalDate.now))))
    )
    when(mockEmailConnector.sendEmail(any())(any(), any())).thenReturn(Future.successful(EmailSent))
    when(mockMinimalConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minPsa))
    doNothing().when(mockAuditService).sendEvent(any())(any(), any())
  }

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onPageLoad(0)
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onSubmit(0)
  }

  private def onSaveAction(userAnswersConnector: UserAnswersCacheConnector): Action[AnyContent] = {
    controller(userAnswersCacheConnector = userAnswersConnector).onSubmit(0)
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

  "send a deauthorise practitioner audit event when psp successfully removed by the PSA" in {
    val result = onSubmitAction(validData, FakeAuthAction)(postRequest)

    when(mockMinimalConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minPsa))

    status(result) mustBe SEE_OTHER

    redirectLocation(result) mustBe Some(onwardRoute.url)

    // scalastyle:off magic.number
    val expectedAuditEvent = PSPDeauthorisationAuditEvent(LocalDate.of(2020,5,1), "A0000000", "A2200005")

    verify(mockAuditService, times(1)).sendEvent(Matchers.eq(expectedAuditEvent))(any(), any())
  }

  "send an email to the PSA email address and send an email audit event when psp successfully removed by the PSA" in {
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

    val expectedAuditEvent = PSPDeauthorisationEmailAuditEvent(
      psaId = "A0000000",
      pspId = "A2200005",
      pstr = pstr,
      emailAddress = minPsa.email
    )
    verify(mockAuditService, times(1)).sendEvent(Matchers.eq(expectedAuditEvent))(any(), any())
  }

  behave like controllerThatSavesUserAnswers(
    saveAction = onSaveAction,
    validRequest = postRequest,
    id = PsaRemovePspDeclarationId(0),
    value = true
  )
}

object PsaRemovePspDeclarationControllerSpec {
  private val schemeName = "test scheme name"
  private val srn = "test srn"
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
    organisationName  = None,
    individualDetails = Some(IndividualDetails(firstName = individualFirstName, middleName = None, lastName = individualLastName)),
    rlsFlag = false,
    deceasedFlag = false
  )

  private val data = Json.obj(
    SchemeSrnId.toString -> srn,
    SchemeNameId.toString -> schemeName,
    SeqAuthorisedPractitionerId.toString -> practitioners,
    PSTRId.toString -> pstr,
    PspRemovalDateId(0).toString -> "2020-05-01"
  )

  private val sessionData: FakeDataRetrievalAction =
    new FakeDataRetrievalAction(Some(data))

  private val validData: FakeDataRetrievalAction =
    new FakeDataRetrievalAction(Some(
      data
        ++
        Json.obj(PsaRemovePspDeclarationId(0).toString -> "true")
    ))

  val postRequest: FakeRequest[AnyContentAsJson] =
    FakeRequest().withJsonBody(Json.obj(
      "value" -> "true"
    ))

  val emptyPostRequest: FakeRequest[AnyContentAsJson] =
    FakeRequest().withJsonBody(Json.obj(
      "value" -> ""
    ))
}
