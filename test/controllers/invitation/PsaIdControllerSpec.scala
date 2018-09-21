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

package controllers.invitation

import connectors.FakeDataCacheConnector
import controllers.actions._
import controllers.behaviours.QuestionPageBehaviours
import forms.invitation.PsaIdFromProvider
import identifiers.{PSAId, PsaNameId}
import models.NormalMode
import play.api.data.Form
import play.api.test.FakeRequest
import utils.UserAnswers
import views.html.invitation.psaId

class PsaIdControllerSpec extends QuestionPageBehaviours {

  val formProvider = new PsaIdFromProvider()
  val form = formProvider()

  val userAnswer = UserAnswers().set(PsaNameId)("xyz").asOpt.value
  val userAnswerWithPsaID = userAnswer.set(PSAId)("A0000000").asOpt.value


  def getDataRetrieval(userAnswer: UserAnswers) = {
    new FakeDataRetrievalAction(Some(userAnswer.json))
  }

  def viewAsString(form: Form[_] = form) = psaId(frontendAppConfig, form, "xyz", NormalMode)(fakeRequest, messages).toString

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PsaIdController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, FakeDataCacheConnector,
      dataRetrievalAction, requiredDateAction, formProvider).onPageLoad(NormalMode)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PsaIdController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, FakeDataCacheConnector,
      dataRetrievalAction, requiredDateAction, formProvider).onSubmit(NormalMode)
  }


  val postRequest = FakeRequest().withJsonBody(userAnswerWithPsaID.json)

  behave like onPageLoadMethod(onPageLoadAction, getDataRetrieval(userAnswer), getDataRetrieval(userAnswerWithPsaID), form, form.fill("A0000000"), viewAsString)

  behave like onSubmitMethod(onSubmitAction, getDataRetrieval(userAnswerWithPsaID), form.bind(Map("psaId" -> "")), viewAsString, postRequest)

  behave like requiredDataMissing(onPageLoadAction, onSubmitAction, getEmptyData)

}
