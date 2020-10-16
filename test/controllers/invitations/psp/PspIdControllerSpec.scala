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
import identifiers.SchemeNameId
import identifiers.SchemeSrnId
import identifiers.invitations.psp.PspId
import identifiers.invitations.psp.PspNameId
import models.NormalMode
import models.SchemeReferenceNumber
import play.api.data.Form
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers
import views.html.invitations.psp.pspId

class PspIdControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new PspIdFormProvider()
  val form: Form[String] = formProvider()
  private val schemeName = "Test Scheme"
  private val srn = "srn"
  private val userAnswer = UserAnswers()
    .set(PspNameId)("xyz").asOpt.value
    .set(SchemeNameId)(schemeName).asOpt.value
    .set(SchemeSrnId)(srn).asOpt.value
  val userAnswerWithPspId: UserAnswers = userAnswer.set(PspId)("00000000").asOpt.value
  private val postRequest = FakeRequest().withJsonBody(userAnswerWithPspId.json)
  private val view = injector.instanceOf[pspId]

  private val returnCall = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber("srn"))


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

  def viewAsString(form: Form[_] = form) = view(form, "xyz", NormalMode, schemeName, returnCall)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, userAnswer.dataRetrievalAction,
    userAnswerWithPspId.dataRetrievalAction, form, form.fill("00000000"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, userAnswerWithPspId.dataRetrievalAction, form.bind(Map("pspId" -> "")), viewAsString, postRequest)

  behave like controllerWithOnPageLoadMethodMissingRequiredData(onPageLoadAction, getEmptyData)

  behave like controllerWithOnSubmitMethodMissingRequiredData(onSubmitAction, getEmptyData)

}
