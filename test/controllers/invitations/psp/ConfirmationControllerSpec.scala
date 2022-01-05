/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.behaviours.ControllerWithNormalPageBehaviours
import identifiers.SchemeNameId
import identifiers.invitations.psp.PspNameId
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import utils.UserAnswers
import views.html.invitations.psp.confirmation

class ConfirmationControllerSpec extends ControllerWithNormalPageBehaviours {


  private val testPspName = "test-psp-name"
  private val testSchemeName = "test-scheme-name"

  private val userAnswer = UserAnswers()
    .set(SchemeNameId)(testSchemeName).asOpt.value
    .set(PspNameId)(testPspName).asOpt.value
    .dataRetrievalAction

  private val confirmationView = injector.instanceOf[confirmation]

  def viewAsString() = confirmationView(testSchemeName, testPspName)(fakeRequest, messages).toString

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new ConfirmationController(
      messagesApi, frontendAppConfig, fakeAuth, dataRetrievalAction, requiredDateAction, FakeUserAnswersCacheConnector,
      controllerComponents, confirmationView).onPageLoad()
  }


  def redirectionCall(): Call = onwardRoute

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, Some(userAnswer), viewAsString)

  "ConfirmationController" when {
    "on PageLoad" must {
      "remove all the data from the cache" in {
        onPageLoadAction(userAnswer, FakeAuthAction)(fakeRequest)

        FakeUserAnswersCacheConnector.verifyAllDataRemoved()
      }
    }
  }

}
