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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import controllers.actions.{DataRequiredActionImpl, FakeAuthAction, FakeUnAuthorisedAction}
import models.MinimalSchemeDetail
import org.joda.time.LocalDate
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.{DateHelper, FakeNavigator, UserAnswers}
import views.html.invitation_success

class InvitationSuccessControllerSpec extends ControllerSpecBase {

  import InvitationSuccessControllerSpec._

  "InvitationSuccessController" must {

    "return 200 Ok and correct content on successful GET" in {

      val fixture = testFixture(this)
      val result = fixture.controller.onPageLoad(testSrn)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(this)

    }

    "calculate the correct invitation expiry date as today's date plus 30 days" in {

      val expected = DateHelper.formatDate(testExpiryDate(frontendAppConfig))

      val fixture = testFixture(this)
      val result = fixture.controller.onPageLoad(testSrn)(fakeRequest)

      contentAsString(result) must include (expected)

    }

    "redirect to Unauthorised when not authenticated on GET" in {

      val fixture = unauthorisedTestFixture(this)
      val result = fixture.controller.onPageLoad(testSrn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)

    }

    "redirect to session expired when there is no user data on GET" in {

      val fixture = noDataTestFixture(this)
      val result = fixture.controller.onPageLoad(testSrn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)

    }

    "redirect to the next page on successful POST" in {

      val fixture = testFixture(this)
      val result = fixture.controller.onSubmit(testSrn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(fixture.navigator.desiredRoute.url)

    }

  }

}

// scalastyle:off magic.number

object InvitationSuccessControllerSpec {

  val testSrn: String = "test-srn"

  def testExpiryDate(config: FrontendAppConfig): LocalDate = {
    LocalDate.now().plusDays(config.invitationExpiryDays)
  }

  private val testInviteeName = "test-invitee-name"
  private val testPstr = "test-pstr"
  private val testSchemeName = "test-scheme-name"
  private val testSchemeDetail = MinimalSchemeDetail(testSrn, Some(testPstr), testSchemeName)

  private val onwardRoute: Call = controllers.routes.IndexController.onPageLoad()
  private val testNavigator: FakeNavigator = new FakeNavigator(onwardRoute)
  private lazy val continue: Call = controllers.routes.InvitationSuccessController.onSubmit(testSrn)

  private def createController(base: ControllerSpecBase, authorised: Boolean, hasData: Boolean): InvitationSuccessController = {

    val authAction =
      if (authorised) {
        FakeAuthAction()
      } else {
        FakeUnAuthorisedAction()
      }

    val dataRetrievalAction =
      if (hasData) {
        UserAnswers()
          .inviteeName(testInviteeName)
          .minimalSchemeDetails(testSchemeDetail)
          .dataRetrievalAction
      } else {
        base.dontGetAnyData
      }

    new InvitationSuccessController(
      base.messagesApi,
      base.frontendAppConfig,
      authAction,
      dataRetrievalAction,
      new DataRequiredActionImpl,
      testNavigator
    )

  }

  trait TestFixture {
    val controller: InvitationSuccessController
    val navigator: FakeNavigator
  }

  private def testFixture(base: ControllerSpecBase, authorised: Boolean, hasData: Boolean): TestFixture =
    new TestFixture {
      override val controller: InvitationSuccessController = createController(base, authorised, hasData)
      override val navigator: FakeNavigator = testNavigator
    }

  def testFixture(base: ControllerSpecBase): TestFixture = testFixture(base, true, true)

  def unauthorisedTestFixture(base: ControllerSpecBase): TestFixture = testFixture(base, false, true)

  def noDataTestFixture(base: ControllerSpecBase): TestFixture = testFixture(base, true, false)

  def viewAsString(base: SpecBase): String =
    invitation_success(
      base.frontendAppConfig,
      testInviteeName,
      testSchemeName,
      testExpiryDate(base.frontendAppConfig),
      continue
    )(
      base.fakeRequest,
      base.messages
    ).toString()

}
