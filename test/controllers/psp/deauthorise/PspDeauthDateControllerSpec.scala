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

import connectors.{FakeUserAnswersCacheConnector, UserAnswersCacheConnector}
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.psp.deauthorise.PspDeauthDateFormProvider
import identifiers.psp.deauthorise
import identifiers.{SchemeNameId, SchemeSrnId, SeqAuthorisedPractitionerId}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito._
import play.api.data.Form
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsJson}
import play.api.test.FakeRequest
import utils.DateHelper._
import views.html.psp.deauthorisation.pspDeauthDate

import java.time.LocalDate

class PspDeauthDateControllerSpec
  extends ControllerWithQuestionPageBehaviours
    with MockitoSugar
    with BeforeAndAfterEach {

  import PspDeauthDateControllerSpec._

  private val formProvider: PspDeauthDateFormProvider = new PspDeauthDateFormProvider()
  private val form = formProvider

  private val view = app.injector.instanceOf[pspDeauthDate]

  def controller(
                  dataRetrievalAction: DataRetrievalAction = sessionData,
                  fakeAuth: AuthAction = FakeAuthAction,
                  userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) =
    new PspDeauthDateController(
      messagesApi = messagesApi,
      userAnswersCacheConnector = userAnswersCacheConnector,
      navigator = navigator,
      authenticate = fakeAuth,
      getData = dataRetrievalAction,
      requireData = requiredDataAction,
      formProvider = formProvider,
      controllerComponents = controllerComponents,
      view = view,
      fakePspSchemeAuthAction
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
    id = deauthorise.PspDeauthDateId(0),
    value = date
  )
}

object PspDeauthDateControllerSpec extends MockitoSugar {
  private val relationshipStartDate = LocalDate.parse("2020-04-01")
  private val schemeName = "test scheme name"
  private val pspName = "PSP Limited Company 1"
  private val srn = "test srn"
  private val date = LocalDate.now()

  val day: Int = LocalDate.now().getDayOfMonth
  val month: Int = LocalDate.now().getMonthValue
  val year: Int = LocalDate.now().getYear

  val dateKeys = Map("pspDeauthDate.day" -> "", "pspDeauthDate.month" -> "", "pspDeauthDate.year" -> "")

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
        Json.obj(deauthorise.PspDeauthDateId(0).toString -> LocalDate.parse("2020-05-01"))
    ))

  val postRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj(
    "pspDeauthDate.day" -> day.toString,
    "pspDeauthDate.month" -> month.toString,
    "pspDeauthDate.year" -> year.toString)
  )

  val emptyPostRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj(
    "pspDeauthDate.day" -> "",
    "pspDeauthDate.month" -> "",
    "pspDeauthDate.year" -> "")
  )
}


