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

package controllers.invitations

import config.FrontendAppConfig
import controllers.actions._
import controllers.behaviours.ControllerWithNormalPageBehaviours
import models.MinimalSchemeDetail
import org.joda.time.LocalDate
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.{DateHelper, UserAnswers}
import views.html.invitations.invitation_success

class InvitationSuccessControllerSpec extends ControllerWithNormalPageBehaviours {

  private val testSrn: String = "test-srn"
  private val testInviteeName = "test-invitee-name"
  private val testPstr = "test-pstr"
  private val testSchemeName = "test-scheme-name"
  private val testSchemeDetail = MinimalSchemeDetail(testSrn, Some(testPstr), testSchemeName)

  private lazy val continue: Call = controllers.invitations.routes.InvitationSuccessController.onSubmit

  private val userAnswer = UserAnswers()
    .inviteeName(testInviteeName)
    .minimalSchemeDetails(testSchemeDetail)
    .dataRetrievalAction

  def viewAsString() = invitation_success(frontendAppConfig,
    testInviteeName,
    testSchemeName,
    testExpiryDate(frontendAppConfig),
    continue)(fakeRequest, messages).toString

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new InvitationSuccessController(
      messagesApi, frontendAppConfig, fakeAuth, dataRetrievalAction, requiredDateAction, navigator).onPageLoad
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new InvitationSuccessController(
      messagesApi, frontendAppConfig, fakeAuth, dataRetrievalAction, requiredDateAction, navigator).onSubmit
  }

  def testExpiryDate(config: FrontendAppConfig): LocalDate = {
    LocalDate.now().plusDays(config.invitationExpiryDays)
  }


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, Some(userAnswer), viewAsString)

  "calculate the correct invitation expiry date as today's date plus 30 days" in {

    val expected = DateHelper.formatDate(testExpiryDate(frontendAppConfig))

    val result = onPageLoadAction(userAnswer, FakeAuthAction())(fakeRequest)

    contentAsString(result) must include(expected)

  }

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData, None, None)

}
