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

import controllers.actions._
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.psa.routes._
import identifiers.{SchemeNameId, SchemeSrnId}
import identifiers.invitations.psp.PspNameId
import models.SchemeReferenceNumber
import play.api.mvc.{Action, AnyContent, Call}
import utils.UserAnswers
import views.html.invitations.psp.pspDoesNotMatch

class PspDoesNotMatchControllerSpec extends ControllerWithNormalPageBehaviours {


  private val testPspName = "test-psp-name"
  private val testSrn = "test-srn"
  private val testSchemeName = "test-scheme-name"

  private lazy val continue: Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(testSrn))

  private val userAnswer = UserAnswers()
    .set(SchemeNameId)(testSchemeName).asOpt.value
    .set(SchemeSrnId)(testSrn).asOpt.value
    .set(PspNameId)(testPspName).asOpt.value
    .dataRetrievalAction

  private val pspDoesNotMatchView = injector.instanceOf[pspDoesNotMatch]

  def viewAsString(): String = pspDoesNotMatchView(testSchemeName, testPspName, continue)(fakeRequest, messages).toString

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PspDoesNotMatchController(
      messagesApi, fakeAuth, dataRetrievalAction, requiredDateAction,
      controllerComponents, pspDoesNotMatchView).onPageLoad()
  }

  def redirectionCall(): Call = onwardRoute

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, Some(userAnswer), () => viewAsString())

}
