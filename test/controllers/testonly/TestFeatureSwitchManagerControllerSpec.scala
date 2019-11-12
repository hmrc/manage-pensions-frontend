/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.testonly

import config.FeatureSwitchManagementService
import connectors.{PensionAdministratorFeatureSwitchConnectorImpl, PensionsSchemeFeatureSwitchConnectorImpl}
import controllers.ControllerSpecBase
import forms.mappings.Mappings
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class TestFeatureSwitchManagerControllerSpec extends ControllerSpecBase with Mappings with MockitoSugar {

  private val fakeFeatureSwitchManagerService = mock[FeatureSwitchManagementService]
  private val fakePensionsSchemeFeatureSwitchConnectorImpl = mock[PensionsSchemeFeatureSwitchConnectorImpl]
  private val fakePensionAdminFeatureSwitchConnectorImpl = mock[PensionAdministratorFeatureSwitchConnectorImpl]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def controller: TestFeatureSwitchManagerController =
    new TestFeatureSwitchManagerController(
      fakeFeatureSwitchManagerService,
      fakePensionsSchemeFeatureSwitchConnectorImpl,
      fakePensionAdminFeatureSwitchConnectorImpl
    )

  val toggleName = "test-toggle"

  "TestFeatureSwitchManager Controller" when {

    "toggle on is called" must {
      "return Ok if the toggle value is changed successfully" in {
        when(fakeFeatureSwitchManagerService.change(any(), any())).thenReturn(true)
        when(fakeFeatureSwitchManagerService.get(any())).thenReturn(true)
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.toggleOn(any())(any(), any())).thenReturn(Future.successful(true))
        when(fakePensionAdminFeatureSwitchConnectorImpl.toggleOn(any())(any(), any())).thenReturn(Future.successful(true))

        when(fakePensionsSchemeFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))
        when(fakePensionAdminFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))

        val result = controller.toggleOn(toggleName)(fakeRequest)
        status(result) mustBe OK
      }

      "return Expectation Failed if changing the frontend toggle value is failed" in {
        when(fakeFeatureSwitchManagerService.change(any(), any())).thenReturn(false)
        when(fakeFeatureSwitchManagerService.get(any())).thenReturn(false)
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.toggleOn(any())(any(), any())).thenReturn(Future.successful(true))
        when(fakePensionAdminFeatureSwitchConnectorImpl.toggleOn(any())(any(), any())).thenReturn(Future.successful(true))

        when(fakePensionsSchemeFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))
        when(fakePensionAdminFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))

        val result = controller.toggleOn(toggleName)(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }

      "return Expectation Failed if changing the toggle value for pensions scheme is failed" in {
        when(fakeFeatureSwitchManagerService.change(any(), any())).thenReturn(true)
        when(fakeFeatureSwitchManagerService.get(any())).thenReturn(true)
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.toggleOn(any())(any(), any())).thenReturn(Future.successful(false))
        when(fakePensionAdminFeatureSwitchConnectorImpl.toggleOn(any())(any(), any())).thenReturn(Future.successful(true))

        when(fakePensionsSchemeFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(false)))
        when(fakePensionAdminFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))

        val result = controller.toggleOn(toggleName)(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }

      "return Expectation Failed if changing the toggle value for pension administrator is failed" in {
        when(fakeFeatureSwitchManagerService.change(any(), any())).thenReturn(true)
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.toggleOn(any())(any(), any())).thenReturn(Future.successful(true))
        when(fakePensionAdminFeatureSwitchConnectorImpl.toggleOn(any())(any(), any())).thenReturn(Future.successful(false))

        when(fakePensionsSchemeFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))
        when(fakePensionAdminFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(false)))

        val result = controller.toggleOn(toggleName)(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }
    }

    "toggle off is called" must {
      "return No Content if the toggle value is changed successfully" in {
        when(fakeFeatureSwitchManagerService.change(any(), any())).thenReturn(true)
        when(fakeFeatureSwitchManagerService.get(any())).thenReturn(false)
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.toggleOff(any())(any(), any())).thenReturn(Future.successful(true))
        when(fakePensionAdminFeatureSwitchConnectorImpl.toggleOff(any())(any(), any())).thenReturn(Future.successful(true))

        when(fakePensionsSchemeFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(false)))
        when(fakePensionAdminFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(false)))

        val result = controller.toggleOff(toggleName)(fakeRequest)
        status(result) mustBe OK
      }

      "return Expectation Failed if changing the frontend toggle value is failed" in {
        when(fakeFeatureSwitchManagerService.change(any(), any())).thenReturn(false)
        when(fakeFeatureSwitchManagerService.get(any())).thenReturn(false)
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.toggleOff(any())(any(), any())).thenReturn(Future.successful(true))
        when(fakePensionAdminFeatureSwitchConnectorImpl.toggleOff(any())(any(), any())).thenReturn(Future.successful(true))

        when(fakePensionsSchemeFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))
        when(fakePensionAdminFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))

        val result = controller.toggleOff(toggleName)(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }
      "return Expectation Failed if changing the pensions scheme toggle value is failed" in {
        when(fakeFeatureSwitchManagerService.change(any(), any())).thenReturn(true)
        when(fakeFeatureSwitchManagerService.get(any())).thenReturn(true)
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.toggleOff(any())(any(), any())).thenReturn(Future.successful(false))
        when(fakePensionAdminFeatureSwitchConnectorImpl.toggleOff(any())(any(), any())).thenReturn(Future.successful(true))

        when(fakePensionsSchemeFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(false)))
        when(fakePensionAdminFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))

        val result = controller.toggleOff(toggleName)(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }
      "return Expectation Failed if changing the pension administrator toggle value is failed" in {
        when(fakeFeatureSwitchManagerService.change(any(), any())).thenReturn(true)
        when(fakeFeatureSwitchManagerService.get(any())).thenReturn(true)
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.toggleOff(any())(any(), any())).thenReturn(Future.successful(true))
        when(fakePensionAdminFeatureSwitchConnectorImpl.toggleOff(any())(any(), any())).thenReturn(Future.successful(false))

        when(fakePensionsSchemeFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(true)))
        when(fakePensionAdminFeatureSwitchConnectorImpl.get(any())(any(), any())).thenReturn(Future.successful(Some(false)))

        val result = controller.toggleOff(toggleName)(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }
    }

    "reset is called" must {
      "return OK when reset is done successfully" in {
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.reset(any())(any(), any())).thenReturn(Future.successful(true))
        when(fakePensionAdminFeatureSwitchConnectorImpl.reset(any())(any(), any())).thenReturn(Future.successful(true))
        val result = controller.reset(toggleName)(fakeRequest)
        status(result) mustBe OK
      }

      "return Expectation Failed if resetting the frontend toggle value is failed" in {
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.reset(any())(any(), any())).thenReturn(Future.successful(true))
        when(fakePensionAdminFeatureSwitchConnectorImpl.reset(any())(any(), any())).thenReturn(Future.successful(true))
        val result = controller.toggleOff(toggleName)(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }

      "return Expectation Failed if resetting the pensions scheme toggle value is failed" in {
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.reset(any())(any(), any())).thenReturn(Future.successful(false))
        when(fakePensionAdminFeatureSwitchConnectorImpl.reset(any())(any(), any())).thenReturn(Future.successful(true))
        val result = controller.toggleOff(toggleName)(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }
      "return Expectation Failed if resetting the pension administrator toggle value is failed" in {
        when(fakePensionsSchemeFeatureSwitchConnectorImpl.reset(any())(any(), any())).thenReturn(Future.successful(true))
        when(fakePensionAdminFeatureSwitchConnectorImpl.reset(any())(any(), any())).thenReturn(Future.successful(false))
        val result = controller.toggleOff(toggleName)(fakeRequest)
        status(result) mustBe EXPECTATION_FAILED
      }
    }
  }
}
