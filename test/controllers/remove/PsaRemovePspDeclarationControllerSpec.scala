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

package controllers.remove

import java.time.LocalDate

import connectors.{FakeUserAnswersCacheConnector, PspConnector, UserAnswersCacheConnector}
import controllers.actions.{AuthAction, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.remove.PsaRemovePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.remove.PsaRemovePspDeclarationId
import identifiers.{SchemeNameId, SchemeSrnId, SeqAuthorisedPractitionerId}
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsJson}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.remove.psaRemovePspDeclaration

import scala.concurrent.Future

class PsaRemovePspDeclarationControllerSpec extends ControllerWithQuestionPageBehaviours with MockitoSugar with BeforeAndAfterEach {

  import PsaRemovePspDeclarationControllerSpec._

  private val formProvider: PsaRemovePspDeclarationFormProvider = new PsaRemovePspDeclarationFormProvider()
  private val form = formProvider

  private val view = app.injector.instanceOf[psaRemovePspDeclaration]

  private val mockPspConnector: PspConnector = mock[PspConnector]

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
      controllerComponents = stubMessagesControllerComponents(),
      view = view
    )

  override def beforeEach(): Unit = {
    reset(mockPspConnector)
    when(mockPspConnector.deAuthorise(any(), any())(any(), any())).thenReturn(
      Future.successful(HttpResponse.apply(200, Json.stringify(Json.obj("processingDate" -> LocalDate.now))))
    )
  }

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onPageLoad(0)
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onSubmit(0)
  }

  private def onSaveAction(userAnswersConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector): Action[AnyContent] = {
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
    form = form().bind(Map("value" -> "true")),
    errorView = viewAsStringPostRequest,
    postRequest = postRequest,
    emptyPostRequest = Some(emptyPostRequest)
  )

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

  private val data = Json.obj(
    SchemeSrnId.toString -> srn,
    SchemeNameId.toString -> schemeName,
    SeqAuthorisedPractitionerId.toString -> practitioners,
    PSTRId.toString -> pstr
  )

  private val sessionData: FakeDataRetrievalAction =
    new FakeDataRetrievalAction(Some(data))

  private val validData: FakeDataRetrievalAction =
    new FakeDataRetrievalAction(Some(
      data ++
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
