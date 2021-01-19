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

package controllers.invitations.psp

import connectors.FakeUserAnswersCacheConnector
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.psp.PspNameFormProvider
import identifiers.SchemeNameId
import identifiers.SchemeSrnId
import identifiers.invitations.psp.PspNameId
import models.NormalMode
import models.SchemeReferenceNumber
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import utils.UserAnswers
import utils._
import views.html.invitations.psp.pspName

class PspNameControllerSpec extends ControllerWithQuestionPageBehaviours {

  private val formProvider = new PspNameFormProvider()
  private val form = formProvider()
  private val schemeName = "Test Scheme"
  private val srn = "srn"
  private val userAnswer = UserAnswers()
    .set(SchemeNameId)(schemeName).asOpt.value
    .set(SchemeSrnId)(srn).asOpt.value
  private val userAnswerWithPspName = UserAnswers()
    .set(PspNameId)("xyz").asOpt.value
    .set(SchemeNameId)(schemeName).asOpt.value
    .set(SchemeSrnId)(srn).asOpt.value

  private val postRequest = fakeRequest.withJsonBody(userAnswerWithPspName.json)
  private val pspNameView = injector.instanceOf[pspName]


  private val returnCall = controllers.routes.PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber("srn"))


  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PspNameController(
      frontendAppConfig, messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, requiredDataAction, formProvider, controllerComponents, pspNameView).onPageLoad(NormalMode)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PspNameController(
      frontendAppConfig, messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, requiredDataAction, formProvider, controllerComponents, pspNameView).onSubmit(NormalMode)
  }

  private def viewAsString(form: Form[_]): String = pspNameView(form, NormalMode, schemeName, returnCall)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, userAnswer.dataRetrievalAction, userAnswerWithPspName.dataRetrievalAction,
    form, form.fill("xyz"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, userAnswerWithPspName.dataRetrievalAction, form.bind(Map("pspName" -> "")),
    viewAsString, postRequest)

}

