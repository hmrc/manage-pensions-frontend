/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, _}
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.AdviserDetailsFormProvider
import identifiers.invitations.AdviserNameId
import models.NormalMode
import play.api.data.Form
import play.api.test.FakeRequest
import utils.{FakeNavigator, UserAnswers}
import views.html.invitations.adviserDetails

class AdviserDetailsControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new AdviserDetailsFormProvider()
  val form = formProvider()
  val userAnswer = UserAnswers().adviserName("test")
  val postRequest = FakeRequest().withJsonBody(userAnswer.json)

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new AdviserDetailsController(
      frontendAppConfig, messagesApi,fakeAuth, new FakeNavigator(onwardRoute), dataRetrievalAction,
      requiredDataAction, formProvider, FakeUserAnswersCacheConnector).onPageLoad(NormalMode)
  }


  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new AdviserDetailsController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction,
      requiredDataAction, formProvider, FakeUserAnswersCacheConnector).onSubmit(NormalMode)
  }

  private def viewAsString(form: Form[_] = form) = adviserDetails(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, userAnswer.dataRetrievalAction, form, form.fill("test"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData, form.bind(Map(AdviserNameId.toString -> "")), viewAsString, postRequest)

}

