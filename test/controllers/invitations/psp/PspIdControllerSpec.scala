/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.invitations.psp

import connectors.FakeUserAnswersCacheConnector
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.psp.PspIdFormProvider
import identifiers.invitations.psp.{PspId, PspNameId}
import models.NormalMode
import play.api.data.Form
import play.api.mvc.{AnyContent, AnyContentAsJson, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers
import views.html.invitations.psp.pspId

class PspIdControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new PspIdFormProvider()
  val form: Form[String] = formProvider()
  val userAnswer: UserAnswers = UserAnswers().set(PspNameId)("xyz").asOpt.value
  val userAnswerWithPspId: UserAnswers = userAnswer.set(PspId)("A0000000").asOpt.value
  private val postRequest = FakeRequest().withJsonBody(userAnswerWithPspId.json)
  private val view = injector.instanceOf[pspId]

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PspIdController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, FakeUserAnswersCacheConnector,
      dataRetrievalAction, requiredDataAction, formProvider, stubMessagesControllerComponents(), view).onPageLoad(NormalMode)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new PspIdController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, FakeUserAnswersCacheConnector,
      dataRetrievalAction, requiredDataAction, formProvider, stubMessagesControllerComponents(), view).onSubmit(NormalMode)
  }

  def viewAsString(form: Form[_] = form) = view(form, "xyz", NormalMode)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, userAnswer.dataRetrievalAction,
    userAnswerWithPspId.dataRetrievalAction, form, form.fill("A0000000"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, userAnswerWithPspId.dataRetrievalAction, form.bind(Map("pspId" -> "")), viewAsString, postRequest)

  behave like controllerWithOnPageLoadMethodMissingRequiredData(onPageLoadAction, getEmptyData)

  behave like controllerWithOnSubmitMethodMissingRequiredData(onSubmitAction, getEmptyData)

}
