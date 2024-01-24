/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.invitations.psa

import base.SpecBase
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, FakeAuthAction, FakeUnAuthorisedAction}
import models.MinimalSchemeDetail
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.{FakeNavigator, UserAnswers}
import views.html.invitations.psa.psa_already_associated

class PsaAlreadyAssociatedControllerSpec extends ControllerSpecBase {

  import PsaAlreadyAssociatedControllerSpec._

  "PsaAlreadyAssociatedController" must {

    "return 200 Ok and correct content on successful GET" in {

      val fixture = testFixture(this)
      val result = fixture.controller.onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(this)

    }

    "redirect to Unauthorised when not authenticated on GET" in {

      val fixture = unauthorisedTestFixture(this)
      val result = fixture.controller.onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)

    }

    "redirect to session expired when there is no user data on GET" in {

      val fixture = noDataTestFixture(this)
      val result = fixture.controller.onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad.url)

    }
  }
}


object PsaAlreadyAssociatedControllerSpec extends ControllerSpecBase {
  val testSrn: String = "test-srn"

  private val testInviteeName = "test-invitee-name"
  private val testPstr = "test-pstr"
  private val testSchemeName = "test-scheme-name"
  private val testSchemeDetail = MinimalSchemeDetail(testSrn, Some(testPstr), testSchemeName)

  private val onwardRoute: Call = controllers.routes.IndexController.onPageLoad
  private val testNavigator: FakeNavigator = new FakeNavigator(onwardRoute)
  private val view = injector.instanceOf[psa_already_associated]

  private def createController(base: ControllerSpecBase, authorised: Boolean, hasData: Boolean): PsaAlreadyAssociatedController = {

    val authAction =
      if (authorised) {
        FakeAuthAction
      } else {
        FakeUnAuthorisedAction
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

    new PsaAlreadyAssociatedController(
      base.messagesApi,
      authAction,
      dataRetrievalAction,
      new DataRequiredActionImpl,
      controllerComponents,
      view
    )

  }

  trait TestFixture {
    val controller: PsaAlreadyAssociatedController
    val navigator: FakeNavigator
  }

  private def testFixture(base: ControllerSpecBase, authorised: Boolean, hasData: Boolean): TestFixture =
    new TestFixture {
      override val controller: PsaAlreadyAssociatedController = createController(base, authorised, hasData)
      override val navigator: FakeNavigator = testNavigator
    }

  def testFixture(base: ControllerSpecBase): TestFixture = testFixture(base, authorised = true, hasData = true)

  def unauthorisedTestFixture(base: ControllerSpecBase): TestFixture = testFixture(base, authorised = false, hasData = true)

  def noDataTestFixture(base: ControllerSpecBase): TestFixture = testFixture(base, authorised = true, hasData = false)

  def viewAsString(base: SpecBase): String =
    view(
      testInviteeName,
      testSchemeName
    )(
      base.fakeRequest,
      base.messages
    ).toString()

}
