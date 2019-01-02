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
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.PsaNameFormProvider
import models.NormalMode
import play.api.data.Form
import utils.{UserAnswers, _}
import views.html.invitations.psaName

class PsaNameControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new PsaNameFormProvider()
  val form = formProvider()
  val userAnswer = UserAnswers().inviteeName("xyz")
  val postRequest = fakeRequest.withJsonBody(userAnswer.json)

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PsaNameController(
      frontendAppConfig, messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, requiredDataAction, formProvider).onPageLoad(NormalMode)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PsaNameController(
      frontendAppConfig, messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, requiredDataAction, formProvider).onSubmit(NormalMode)
  }

  def viewAsString(form: Form[_]) = psaName(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, userAnswer.dataRetrievalAction, form, form.fill("xyz"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData, form.bind(Map("psaName" -> "")), viewAsString, postRequest)

}

