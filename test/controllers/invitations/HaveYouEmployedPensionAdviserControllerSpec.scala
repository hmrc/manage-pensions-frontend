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
import forms.invitations.HaveYouEmployedPensionAdviserFormProvider
import identifiers.invitations.HaveYouEmployedPensionAdviserId
import models.NormalMode
import play.api.data.Form
import play.api.test.FakeRequest
import utils.UserAnswers
import views.html.invitations.haveYouEmployedPensionAdviser

class HaveYouEmployedPensionAdviserControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new HaveYouEmployedPensionAdviserFormProvider()
  val form = formProvider()
  val userAnswer = UserAnswers().employedPensionAdviserId(true)
  val postRequest = FakeRequest().withJsonBody(userAnswer.json)

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new HaveYouEmployedPensionAdviserController(
      frontendAppConfig, fakeAuth, messagesApi, navigator,formProvider,
      FakeUserAnswersCacheConnector, dataRetrievalAction, requiredDateAction).onPageLoad(NormalMode)
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new HaveYouEmployedPensionAdviserController(
      frontendAppConfig, fakeAuth, messagesApi, navigator, formProvider,
      FakeUserAnswersCacheConnector, dataRetrievalAction, requiredDateAction).onSubmit(NormalMode)
  }

 def viewAsString(form: Form[Boolean] = form) = haveYouEmployedPensionAdviser(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, userAnswer.dataRetrievalAction, form, form.fill(true), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData,
    form.bind(Map(HaveYouEmployedPensionAdviserId.toString -> "")), viewAsString, postRequest)

}


