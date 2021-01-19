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

package controllers

import connectors.scheme.{ListOfSchemesConnector, PensionSchemeVarianceLockConnector, SchemeDetailsConnector}
import connectors.{FakeUserAnswersCacheConnector, FrontendConnector}
import controllers.actions.FakeAuthAction
import identifiers.SchemeStatusId
import models.SchemeStatus.Rejected
import models.{SchemeReferenceNumber, VarianceLock}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{contentAsString, _}
import play.twirl.api.Html
import services.PsaSchemeDashboardService
import views.html.psaSchemeDashboard

import scala.concurrent.Future

class PsaSchemeDashboardControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  import services.PsaSchemeDashboardServiceSpec._

  val psaSchemeDashboardView: psaSchemeDashboard = app.injector.instanceOf[psaSchemeDashboard]
  private val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val mockFrontendConnector: FrontendConnector = mock[FrontendConnector]
  private val fakeListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val fakeSchemeLockConnector: PensionSchemeVarianceLockConnector = mock[PensionSchemeVarianceLockConnector]
  private val mockService: PsaSchemeDashboardService = mock[PsaSchemeDashboardService]

  private val schemeName = "Benefits Scheme"
  private val pstr = Some("10000678RE")
  private val openDate = Some("10 October 2012")
  private val srn = SchemeReferenceNumber("S1000000456")
  private val aftHtml = Html("test-aft-html")

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
      mockFrontendConnector
    )
  }

  override def beforeEach(): Unit = {
    reset(fakeSchemeDetailsConnector, fakeListOfSchemesConnector, fakeSchemeLockConnector, mockService)
    when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(eqTo("A0000000"), any())(any(), any()))
      .thenReturn(Future.successful(Some(VarianceLock)))
  }

   "PsaSchemeDashboardController" must {
    "return OK and the correct view for a GET and NO financial info html if status is NOT open" in {
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

    "return OK and the correct view for a GET and scheme is open" in {
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
