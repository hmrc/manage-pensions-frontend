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

package controllers.psa.remove

import connectors.{FakeUserAnswersCacheConnector, UserAnswersCacheConnector}
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.remove.psa.ConfirmRemovePsaFormProvider
import identifiers.remove.psa.ConfirmRemovePsaId
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import utils.{UserAnswerOps, UserAnswers}
import views.html.remove.psa.confirmRemovePsa

class ConfirmRemovePsaControllerSpec extends ControllerWithQuestionPageBehaviours {

  import ConfirmRemovePsaControllerSpec._
  val view = injector.instanceOf[confirmRemovePsa]

  def controller(dataRetrievalAction: DataRetrievalAction = data, fakeAuth: AuthAction = FakeAuthAction,
                 userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) = new ConfirmRemovePsaController(
    frontendAppConfig, fakeAuth, messagesApi, navigator, formProvider,
    userAnswersCacheConnector, dataRetrievalAction, requiredDataAction, controllerComponents, view)

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onPageLoad()
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {
    controller(dataRetrievalAction, fakeAuth).onSubmit()
  }

  private def onSaveAction(userAnswersConnector: UserAnswersCacheConnector): Action[AnyContent] = {
    controller(userAnswersCacheConnector = userAnswersConnector).onSubmit()
  }

  private def viewAsString(form: Form[Boolean]) = view(form, schemeName,
    srn, psaName)(fakeRequest, messages).toString

  behave like controllerWithOnPageLoadMethod(onPageLoadAction,
    userAnswer.dataRetrievalAction, validData, form, form.fill(true), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, data, form.bind(Map("value" -> "")), viewAsString, postRequest)

  behave like controllerThatSavesUserAnswers(onSaveAction, postRequest, ConfirmRemovePsaId, true)
}

object ConfirmRemovePsaControllerSpec {
  private val formProvider = new ConfirmRemovePsaFormProvider()
  private val form = formProvider()
  private val postRequest = FakeRequest().withJsonBody(Json.obj("value" -> true))
  private val schemeName = "test scheme name"
  private val srn = "test srn"
  private val psaName = "test psa name"

  private val userAnswer = UserAnswers().schemeName(schemeName).srn(srn).psaName(psaName)
  private val data = userAnswer.dataRetrievalAction
  private val validData = userAnswer.confirmRemovePsa(true).dataRetrievalAction
}

