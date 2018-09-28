/*
 * Copyright 2018 HM Revenue & Customs
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
import forms.invitations.PsaIdFromProvider
import models.NormalMode
import play.api.data.Form
import play.api.test.FakeRequest
import utils.UserAnswers
import views.html.invitations.psaId

class PsaIdControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new PsaIdFromProvider()
  val form = formProvider()
  val userAnswer = UserAnswers().inviteeName("xyz")
  val userAnswerWithPsaId = userAnswer.inviteeId("A0000000")
  val postRequest = FakeRequest().withJsonBody(userAnswerWithPsaId.json)


  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PsaIdController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, FakeUserAnswersCacheConnector,
      dataRetrievalAction, requiredDateAction, formProvider).onPageLoad(NormalMode)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PsaIdController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, FakeUserAnswersCacheConnector,
      dataRetrievalAction, requiredDateAction, formProvider).onSubmit(NormalMode)
  }

  def viewAsString(form: Form[_] = form) = psaId(frontendAppConfig, form, "xyz", NormalMode)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, userAnswer.dataRetrievalAction,
    userAnswerWithPsaId.dataRetrievalAction, form, form.fill("A0000000"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, userAnswerWithPsaId.dataRetrievalAction, form.bind(Map("psaId" -> "")), viewAsString, postRequest)

  behave like controllerWithOnPageLoadMethodMissingRequiredData(onPageLoadAction, getEmptyData)

  behave like controllerWithOnSubmitMethodMissingRequiredData(onSubmitAction, getEmptyData)

}
