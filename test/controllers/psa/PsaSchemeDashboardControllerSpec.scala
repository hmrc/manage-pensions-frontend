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

package controllers.psa

import config.FrontendAppConfig
import connectors.admin.MinimalConnector
import connectors.scheme.{ListOfSchemesConnector, PensionSchemeVarianceLockConnector, SchemeDetailsConnector}
import connectors.{FakeUserAnswersCacheConnector, FrontendConnector}
import controllers.ControllerSpecBase
import controllers.actions.FakeAuthAction
import identifiers.SchemeStatusId
import models.SchemeStatus.Rejected
import models.{MinimalPSAPSP, SchemeReferenceNumber, VarianceLock}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{contentAsString, _}
import play.twirl.api.Html
import services.PsaSchemeDashboardService
import views.html.psa.psaSchemeDashboard

import scala.concurrent.Future

class PsaSchemeDashboardControllerSpec
  extends ControllerSpecBase
    with MockitoSugar
    with BeforeAndAfterEach {

  import services.PsaSchemeDashboardServiceSpec._

  val psaSchemeDashboardView: psaSchemeDashboard = app.injector.instanceOf[psaSchemeDashboard]
  private val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val mockFrontendConnector: FrontendConnector = mock[FrontendConnector]
  private val fakeListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val fakeSchemeLockConnector: PensionSchemeVarianceLockConnector = mock[PensionSchemeVarianceLockConnector]
  private val mockService: PsaSchemeDashboardService = mock[PsaSchemeDashboardService]

  private val schemeName = "Benefits Scheme"
  private val srn = SchemeReferenceNumber("S1000000456")
  private val aftHtml = Html("test-aft-html")

  private val mockMinimalPsaConnector: MinimalConnector =
    mock[MinimalConnector]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  def controller(): PsaSchemeDashboardController = {
    new PsaSchemeDashboardController(
      messagesApi,
      fakeSchemeDetailsConnector,
      fakeListOfSchemesConnector,
      fakeSchemeLockConnector,
      FakeAuthAction,
      FakeUserAnswersCacheConnector,
      controllerComponents,
      mockService,
      psaSchemeDashboardView,
      mockFrontendConnector,
      mockMinimalPsaConnector,
      mockAppConfig
    )
  }

  private val dummyUrl = "dummy"
  private val psaName: String = "Test Psa Name"

  private def minimalPSAPSP(rlsFlag: Boolean = false, deceasedFlag: Boolean = false) = MinimalPSAPSP(
    email = "",
    isPsaSuspended = false,
    organisationName = Some(psaName),
    individualDetails = None,
    rlsFlag = rlsFlag,
    deceasedFlag = deceasedFlag
  )

  override def beforeEach(): Unit = {
    reset(fakeSchemeDetailsConnector, fakeListOfSchemesConnector, fakeSchemeLockConnector, mockService)
    when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(eqTo("A0000000"), any())(any(), any()))
      .thenReturn(Future.successful(Some(VarianceLock)))
  }

  "PsaSchemeDashboardController" must {
    "return OK and the correct view for a GET and NO financial info html if status is NOT open" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      val ua = userAnswers.set(SchemeStatusId)(Rejected.value).asOpt.get
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(ua))
      when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))
      when(mockService.cards(any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Seq(schemeCard(), psaCard(), pspCard())))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK

      val expected = psaSchemeDashboardView(schemeName, aftHtml = Html(""),
        Seq(schemeCard(), psaCard(), pspCard()))(fakeRequest, messages).toString()
      contentAsString(result) mustBe expected
    }

    "return redirect to update contact page when rls flag is true but deceased flag is false" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPSAPSP(rlsFlag = true)))
      when(mockAppConfig.psaUpdateContactDetailsUrl).thenReturn(dummyUrl)
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "return redirect to contact hmrc page when rls flag is true and deceased flag is true" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPSAPSP(rlsFlag = true, deceasedFlag = true)))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ContactHMRCController.onPageLoad().url)
    }

    "return OK and the correct view for a GET and scheme is open" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(userAnswers))
      when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))
      when(mockService.cards(any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Seq(schemeCard(), psaCard(), pspCard())))
      when(mockFrontendConnector.retrieveAftPartial(any())(any(), any())).thenReturn(Future(aftHtml))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK

      val expected = psaSchemeDashboardView(schemeName, aftHtml = aftHtml,
        Seq(schemeCard(), psaCard(), pspCard()))(fakeRequest, messages).toString()
      contentAsString(result) mustBe expected
    }
  }
}
