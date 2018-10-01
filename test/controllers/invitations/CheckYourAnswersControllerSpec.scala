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

import connectors.InvitationConnector
import controllers.actions.{AuthAction, DataRetrievalAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import models.{AcceptedInvitation, Invitation, MinimalSchemeDetail}
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import utils.{CheckYourAnswersFactory, UserAnswers}
import viewmodels.AnswerSection
import views.html.check_your_answers

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersControllerSpec extends ControllerWithNormalPageBehaviours {

  private val testSrn: String = "test-srn"
  private val testPstr = "test-pstr"
  private val testSchemeName = "test-scheme-name"
  private val testSchemeDetail = MinimalSchemeDetail(testSrn, Some(testPstr), testSchemeName)

  private lazy val continue: Call = controllers.invitations.routes.InvitationSuccessController.onSubmit(testSrn)

  private val userAnswerOnPageLoad = UserAnswers()
    .minimalSchemeDetails(testSchemeDetail)
    .dataRetrievalAction

  private val checkYourAnswersFactory = new CheckYourAnswersFactory()

  def call: Call = controllers.invitations.routes.CheckYourAnswersController.onSubmit()

  def viewAsString() = check_your_answers(frontendAppConfig, Seq(AnswerSection(None, Seq())), None, call,
    Some("messages__check__your__answer__main__containt__label"), Some(testSchemeName))(fakeRequest, messages).toString

  class FakeInvitationConnector extends InvitationConnector {
    override def invite(invitation: Invitation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.successful(())

    override def acceptInvite(acceptedInvitation: AcceptedInvitation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.successful(())
  }

  val userAnswerOnSubmit = UserAnswers()
    .inviteeName("test invitee name")
    .inviteeId("psaId")
    .minimalSchemeDetails(testSchemeDetail)
    .dataRetrievalAction

  object FakeInvitationConnector extends FakeInvitationConnector

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new CheckYourAnswersController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction, requiredDataAction, FakeInvitationConnector, checkYourAnswersFactory).onPageLoad()
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction , fakeAuth: AuthAction) = {

    new CheckYourAnswersController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction, requiredDataAction, FakeInvitationConnector, checkYourAnswersFactory).onSubmit()
  }

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, Some(userAnswerOnPageLoad), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData,  Some(userAnswerOnSubmit))

}
