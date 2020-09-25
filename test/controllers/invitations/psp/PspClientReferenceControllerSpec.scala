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
import forms.invitations.psp.{PspClientReferenceFormProvider, PspIdFormProvider}
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import models.NormalMode
import models.invitations.psp.ClientReference
import models.invitations.psp.ClientReference._
import play.api.data.Form
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers
import views.html.invitations.psp.pspClientReference

class PspClientReferenceControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new PspClientReferenceFormProvider()
  val form: Form[ClientReference] = formProvider()
  val userAnswer: UserAnswers = UserAnswers().set(PspNameId)("xyz").asOpt.value
  val userAnswerWithPspClientRef: UserAnswers = userAnswer.set(PspClientReferenceId)(HaveClientReference("A0000000")).asOpt.value
  private val postRequest = FakeRequest().withJsonBody(userAnswerWithPspClientRef.json)
  private val view = injector.instanceOf[pspClientReference]

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PspClientReferenceController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, FakeUserAnswersCacheConnector,
      dataRetrievalAction, requiredDataAction, formProvider, stubMessagesControllerComponents(), view).onPageLoad(NormalMode)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PspClientReferenceController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, FakeUserAnswersCacheConnector,
      dataRetrievalAction, requiredDataAction, formProvider, stubMessagesControllerComponents(), view).onSubmit(NormalMode)
  }

  def viewAsString(form: Form[_] = form): String = view(form, "xyz", NormalMode)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, userAnswer.dataRetrievalAction,
    userAnswerWithPspClientRef.dataRetrievalAction, form, form.fill(HaveClientReference("A0000000")), viewAsString)

  behave like controllerWithOnSubmitMethod(
    onSubmitAction,
    userAnswerWithPspClientRef.dataRetrievalAction,
    form.bind(Map("value.yesNo" -> "")),
    viewAsString, postRequest)

  behave like controllerWithOnPageLoadMethodMissingRequiredData(onPageLoadAction, getEmptyData)

  behave like controllerWithOnSubmitMethodMissingRequiredData(onSubmitAction, getEmptyData)

}
