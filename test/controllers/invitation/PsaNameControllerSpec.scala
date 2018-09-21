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
import forms.invitation.PsaNameFormProvider
import identifiers.PsaNameId
import models.NormalMode
import play.api.data.Form
import play.api.libs.json.Json
import utils.{UserAnswers, FakeNavigator}
import views.html.invitation.psaName

class PsaNameControllerSpec extends QuestionPageBehaviours {

  val formProvider = new PsaNameFormProvider()
  val form = formProvider()
  val userAnswer = UserAnswers().set(PsaNameId)("xyz").asOpt.value
  val postRequest = fakeRequest.withJsonBody(userAnswer.json)

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PsaNameController(
      frontendAppConfig, messagesApi, FakeDataCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, requiredDateAction, formProvider).onPageLoad(NormalMode)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PsaNameController(
      frontendAppConfig, messagesApi, FakeDataCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, requiredDateAction, formProvider).onSubmit(NormalMode)
  }


  def viewAsString(form: Form[_]) = psaName(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString

  def validData = Json.obj(PsaNameId.toString -> "xyz")

  def getRelevantData = new FakeDataRetrievalAction(Some(validData))

  behave like onPageLoadMethod(onPageLoadAction, getEmptyData, getRelevantData, form, form.fill("xyz"), viewAsString)

  behave like onSubmitMethod(onSubmitAction, getEmptyData, form.bind(Map("psaName" -> "")), viewAsString, postRequest)

}

