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

import config._
import connectors.UserAnswersCacheConnector
import controllers.actions.DataRetrievalAction
import controllers.actions._
import controllers.routes.ListSchemesController
import models.IndividualDetails
import models.Link
import models.MinimalPSAPSP
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers._
import services.PspDashboardService
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import viewmodels.CardViewModel
import viewmodels.Message
import views.html.schemesOverview

import scala.concurrent.Future

class PspDashboardControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  import PspDashboardControllerSpec._

  private val fakePspDashboardService: PspDashboardService = mock[PspDashboardService]
  private val fakeUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private def minimalPsaDetails(rlsFlag:Boolean): MinimalPSAPSP = MinimalPSAPSP("test@test.com", isPsaSuspended = false, None,
    Some(IndividualDetails("Test", None, "Psp Name")), rlsFlag = rlsFlag)

  private val view: schemesOverview = app.injector.instanceOf[schemesOverview]
  private val dummyUrl = "dummy"

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyDataPsp): PspDashboardController =
    new PspDashboardController(messagesApi, fakePspDashboardService, FakeAuthAction, dataRetrievalAction,
      fakeUserAnswersCacheConnector, stubMessagesControllerComponents(), view, mockAppConfig)

  def viewAsString(): String = view(pspName, tiles, Some(subHeading), Some(returnLink))(fakeRequest, messages).toString

  "PspDashboard Controller" when {
    "onPageLoad" must {
      "return OK and the correct tiles" in {
        when(fakePspDashboardService.getTiles(eqTo(pspId), any())(any()))
          .thenReturn(tiles)
        when(fakePspDashboardService.getPspDetails(eqTo(pspId))(any()))
          .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = false)))
        when(fakeUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Json.obj()))

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to update contact details page when rls flag is true" in {
        when(fakePspDashboardService.getTiles(eqTo(pspId), any())(any()))
          .thenReturn(tiles)
        when(fakePspDashboardService.getPspDetails(eqTo(pspId))(any()))
          .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = true)))
        when(fakeUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Json.obj()))
        when(mockAppConfig.pspUpdateContactDetailsUrl) thenReturn dummyUrl

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(dummyUrl)
      }

    }
  }
}

object PspDashboardControllerSpec extends ControllerSpecBase {
  val pspName = "Test Psp Name"
  private val pspId = "00000000"

  private val practitionerCard: CardViewModel =
    CardViewModel(
      id = "practitioner-card",
      heading = Message("messages__pspDashboard__details_heading"),
      subHeading = Some(Message("messages__pspDashboard__psp_id")),
      subHeadingParam = Some(pspId),
      links = Seq(
        Link("pspLink", frontendAppConfig.pspDetailsUrl, Message("messages__pspDashboard__psp_change")),
        Link("deregister-link", frontendAppConfig.pspDeregisterIndividualUrl, Message("messages__pspDashboard__psp_deregister"))
      )
    )

  private def schemeCard: CardViewModel =
    CardViewModel(
      id = "scheme-card",
      heading = Message("messages__pspDashboard__scheme_heading"),
      links = Seq(Link("search-schemes", ListSchemesController.onPageLoad().url, Message("messages__pspDashboard__search_scheme")))
    )

  private val tiles: Seq[CardViewModel] = Seq(schemeCard, practitionerCard)
  val subHeading: String = Message("messages__pspDashboard__sub_heading")
  private val returnLink: Link = Link("switch-psa", routes.SchemesOverviewController.onPageLoad().url,
    Message("messages__pspDashboard__switch_psa"))
}




