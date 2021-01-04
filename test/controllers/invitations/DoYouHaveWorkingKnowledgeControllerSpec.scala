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

package controllers.invitations

import connectors.FakeUserAnswersCacheConnector
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.DoYouHaveWorkingKnowledgeFormProvider
import models.NormalMode
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.FakeRequest
import utils.FakeNavigator
import utils.UserAnswerOps
import utils.UserAnswers
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.invitations.doYouHaveWorkingKnowledge

class DoYouHaveWorkingKnowledgeControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new DoYouHaveWorkingKnowledgeFormProvider()
  val form = formProvider()
  val userAnswer = UserAnswers().employedPensionAdviserId(true)
  val postRequest = FakeRequest().withJsonBody(Json.obj("value" -> true))
  val data = UserAnswers().employedPensionAdviserId(true).dataRetrievalAction
  private val view = injector.instanceOf[doYouHaveWorkingKnowledge]

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new DoYouHaveWorkingKnowledgeController(
      frontendAppConfig, fakeAuth, messagesApi, navigator, formProvider,
      FakeUserAnswersCacheConnector, dataRetrievalAction, requiredDataAction, stubMessagesControllerComponents(),
      view).onPageLoad(NormalMode)
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new DoYouHaveWorkingKnowledgeController(
      frontendAppConfig, fakeAuth, messagesApi, navigator, formProvider,
      FakeUserAnswersCacheConnector, dataRetrievalAction, requiredDataAction, stubMessagesControllerComponents(),
      view).onSubmit(NormalMode)
  }

  def viewAsString(form: Form[Boolean] = form) = view(form, NormalMode)(fakeRequest, messages).toString

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, userAnswer.dataRetrievalAction, form, form.fill(true), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, data,
    form.bind(Map("value" -> "")), viewAsString, postRequest)
}

