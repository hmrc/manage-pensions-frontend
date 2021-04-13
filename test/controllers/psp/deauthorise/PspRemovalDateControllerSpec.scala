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

package controllers.psp.deauthorise

import connectors.{FakeUserAnswersCacheConnector, UserAnswersCacheConnector}
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import controllers.psp.deauthorise.PspRemovalDateController
import forms.remove.psp.PspRemovalDateFormProvider
import identifiers.remove.psp
import identifiers.remove.psp.PspRemovalDateId
import identifiers.{SchemeNameId, SchemeSrnId, SeqAuthorisedPractitionerId}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito._
import play.api.data.Form
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsJson}
import play.api.test.FakeRequest
import utils.DateHelper._
import views.html.remove.psp.pspRemovalDate

import java.time.LocalDate

class PspRemovalDateControllerSpec
  extends ControllerWithQuestionPageBehaviours
    with MockitoSugar
    with BeforeAndAfterEach {

  import PspRemovalDateControllerSpec._

  private val formProvider: PspRemovalDateFormProvider = new PspRemovalDateFormProvider()
  private val form = formProvider

  private val view = app.injector.instanceOf[pspRemovalDate]

  def controller(
                  dataRetrievalAction: DataRetrievalAction = sessionData,
                  fakeAuth: AuthAction = FakeAuthAction,
                  userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) =
    new PspRemovalDateController(
      messagesApi = messagesApi,
      userAnswersCacheConnector = userAnswersCacheConnector,
      navigator = navigator,
      authenticate = fakeAuth,
      getData = dataRetrievalAction,
      requireData = requiredDataAction,
      formProvider = formProvider,
      controllerComponents = controllerComponents,
      view = view
    )

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onPageLoad(0)
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onSubmit(0)
  }

  private def onSaveAction(userAnswersConnector: UserAnswersCacheConnector): Action[AnyContent] = {
    controller(userAnswersCacheConnector = userAnswersConnector).onSubmit(0)
  }

  private def viewAsString(form: Form[LocalDate]): String =
    view(form, pspName, schemeName, srn, formatDate(relationshipStartDate), 0)(fakeRequest, messages).toString

  private def viewAsStringPostRequest(form: Form[LocalDate]): String =
    view(form, pspName, schemeName, srn, formatDate(relationshipStartDate), 0)(postRequest, messages).toString

  behave like controllerWithOnPageLoadMethodWithoutPrePopulation(
    onPageLoadAction = onPageLoadAction,
    emptyData = sessionData,
    emptyForm = form(relationshipStartDate, "Some error"),
    validView = viewAsString
  )

  behave like controllerWithOnSubmitMethod(
    onSubmitAction = onSubmitAction,
    validData = validData,
    form = form(relationshipStartDate, "Some error").bind(dateKeys),
    errorView = viewAsStringPostRequest,
    postRequest = postRequest,
    emptyPostRequest = Some(emptyPostRequest)
  )

  behave like controllerThatSavesUserAnswers(
    saveAction = onSaveAction,
    validRequest = postRequest,
    id = PspRemovalDateId(0),
    value = date
  )
}

object PspRemovalDateControllerSpec extends MockitoSugar {
  private val relationshipStartDate = LocalDate.parse("2020-04-01")
  private val schemeName = "test scheme name"
  private val pspName = "PSP Limited Company 1"
  private val srn = "test srn"
  private val date = LocalDate.now()

  val day: Int = LocalDate.now().getDayOfMonth
  val month: Int = LocalDate.now().getMonthValue
  val year: Int = LocalDate.now().getYear

  val dateKeys = Map("pspRemovalDate.day" -> "", "pspRemovalDate.month" -> "", "pspRemovalDate.year" -> "")

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
    SeqAuthorisedPractitionerId.toString -> practitioners
  )

  private val sessionData: FakeDataRetrievalAction =
    new FakeDataRetrievalAction(Some(data))

  private val validData: FakeDataRetrievalAction =
    new FakeDataRetrievalAction(Some(
      data ++
        Json.obj(psp.PspRemovalDateId(0).toString -> LocalDate.parse("2020-05-01"))
    ))

  val postRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj(
    "pspRemovalDate.day" -> day.toString,
    "pspRemovalDate.month" -> month.toString,
    "pspRemovalDate.year" -> year.toString)
  )

  val emptyPostRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj(
    "pspRemovalDate.day" -> "",
    "pspRemovalDate.month" -> "",
    "pspRemovalDate.year" -> "")
  )
}


