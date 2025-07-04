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
import forms.psp.deauthorise.ConfirmDeauthPspFormProvider
import identifiers.psp.deauthorise
import identifiers.{SchemeNameId, SchemeSrnId, SeqAuthorisedPractitionerId}
import models.{Index, SchemeReferenceNumber}
import play.api.data.Form
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import views.html.psp.deauthorisation.confirmDeauthorisePsp

class ConfirmDeauthorisePspControllerSpec extends ControllerWithQuestionPageBehaviours {

  import ConfirmDeauthorisePspControllerSpec._

  val view: confirmDeauthorisePsp = injector.instanceOf[confirmDeauthorisePsp]

  def controller(dataRetrievalAction: DataRetrievalAction = sessionData,
                 fakeAuth: AuthAction = FakeAuthAction,
                 userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector
                ) = new ConfirmDeauthorisePspController(
    frontendAppConfig,
    fakeAuth,
    messagesApi,
    navigator,
    formProvider,
    userAnswersCacheConnector,
    dataRetrievalAction,
    requiredDataAction,
    controllerComponents,
    view,
    fakePsaSchemeAuthAction
  )

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onPageLoad(Index(0), srn)
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onSubmit(Index(0), srn)
  }

  private def onSaveAction(userAnswersCacheConnector: UserAnswersCacheConnector): Action[AnyContent] = {
    controller(userAnswersCacheConnector = userAnswersCacheConnector).onSubmit(Index(0), srn)
  }

  private def viewAsString(form: Form[?]) = view(
    form = form,
    schemeName = schemeName,
    srn = srn,
    pspName = "PSP Limited Company 1",
    index = Index(0)
  )(
    using request = fakeRequest,
    messages = messages
  ).toString

  behave like controllerWithOnPageLoadMethod(
    onPageLoadAction = onPageLoadAction,
    emptyData = sessionData,
    validData = validData,
    emptyForm = form,
    preparedForm = form.fill(true),
    validView = viewAsString
  )

  behave like controllerWithOnSubmitMethod(
    onSubmitAction = onSubmitAction,
    validData = validData,
    form = form.bind(Map("value" -> "")),
    errorView = viewAsString,
    postRequest = postRequest
  )

  behave like controllerThatSavesUserAnswers(
    saveAction = onSaveAction,
    validRequest = postRequest,
    id = deauthorise.ConfirmDeauthorisePspId(0),
    value = true
  )
}

object ConfirmDeauthorisePspControllerSpec {
  private val formProvider = new ConfirmDeauthPspFormProvider()
  private val form = formProvider()
  private val postRequest = FakeRequest().withJsonBody(Json.obj("value" -> true))
  private val schemeName = "test scheme name"
  val srn: SchemeReferenceNumber = SchemeReferenceNumber("AB123456C")

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
      Json.obj(deauthorise.ConfirmDeauthorisePspId(0).toString -> true)
    ))
}



