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

package controllers.psp

import config._
import connectors.{SessionDataCacheConnector, UserAnswersCacheConnector}
import controllers.ControllerSpecBase
import controllers.actions._
import controllers.psp.routes._
import controllers.routes._
import identifiers.AdministratorOrPractitionerId
import models.AdministratorOrPractitioner.Practitioner
import models.{IndividualDetails, Link, MinimalPSAPSP}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.test.Helpers.{contentAsString, _}
import services.PspDashboardService
import utils.UserAnswers
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}
import views.html.pspDashboard

import scala.concurrent.Future

class PspDashboardControllerSpec
  extends ControllerSpecBase
    with MockitoSugar
    with BeforeAndAfterEach {

  import PspDashboardControllerSpec._

  private val mockPspDashboardService: PspDashboardService = mock[PspDashboardService]
  private val mockUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val mockSessionDataCacheConnector: SessionDataCacheConnector = mock[SessionDataCacheConnector]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private def minimalPsaDetails(rlsFlag: Boolean, deceasedFlag: Boolean): MinimalPSAPSP =
    MinimalPSAPSP(
      email = "test@test.com",
      isPsaSuspended = false,
      organisationName = None,
      individualDetails = Some(IndividualDetails("Test", None, "Psp Name")),
      rlsFlag = rlsFlag,
      deceasedFlag = deceasedFlag
    )

  private val view: pspDashboard = app.injector.instanceOf[pspDashboard]
  private val dummyUrl = "dummy"

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyDataPsp): PspDashboardController =
    new PspDashboardController(
      messagesApi = messagesApi,
      service = mockPspDashboardService,
      authenticate = FakeAuthAction,
      getData = dataRetrievalAction,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      sessionDataCacheConnector = mockSessionDataCacheConnector,
      controllerComponents = controllerComponents,
      view = view,
      config = mockAppConfig
    )

  def viewAsString(): String = view(
    name = pspName,
    title = "site.psp",
    cards = tiles,
    subHeading = Some(subHeading),
    returnLink = Some(returnLink)
  )(
    fakeRequest,
    messages
  ).toString

  private val practitionerCard: CardViewModel =
    CardViewModel(
      id = "practitioner-card",
      heading = Message("messages__pspDashboard__details_heading"),
      subHeadings = Seq(
        CardSubHeading(
          subHeading = Message("messages__pspDashboard__psp_id"),
          subHeadingClasses = "heading-small card-sub-heading",
          subHeadingParams = Seq(
            CardSubHeadingParam(
              subHeadingParam = pspId,
              subHeadingParamClasses = "font-small")))),
      links = Seq(
        Link(
          id = "pspLink",
          url = frontendAppConfig.pspDetailsUrl,
          linkText = Message("messages__pspDashboard__psp_change")
        ),
        Link(
          id = "deregister-link",
          url = frontendAppConfig.pspDeregisterIndividualUrl,
          linkText = Message("messages__pspDashboard__psp_deregister")
        )
      )
    )

  private def schemeCard: CardViewModel =
    CardViewModel(
      id = "scheme-card",
      heading = Message("messages__pspDashboard__scheme_heading"),
      links = Seq(
        Link(
          id = "search-schemes",
          url = controllers.psa.routes.ListSchemesController.onPageLoad().url,
          linkText = Message("messages__pspDashboard__search_scheme")
        )
      )
    )

  private val tiles: Seq[CardViewModel] = Seq(schemeCard, practitionerCard)
  val subHeading: String = Message("messages__pspDashboard__sub_heading")

  "PspDashboard Controller" when {
    "onPageLoad" must {
      "return OK and the correct tiles" in {
        when(mockPspDashboardService.getTiles(eqTo(pspId), any())(any()))
          .thenReturn(tiles)
        when(mockPspDashboardService.getPspDetails(eqTo(pspId))(any()))
          .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = false, deceasedFlag = false)))
        when(mockUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Json.obj()))

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to update contact details page when rls flag is true" in {
        when(mockPspDashboardService.getTiles(eqTo(pspId), any())(any()))
          .thenReturn(tiles)
        when(mockPspDashboardService.getPspDetails(eqTo(pspId))(any()))
          .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = true, deceasedFlag = false)))
        when(mockUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Json.obj()))
        when(mockAppConfig.pspUpdateContactDetailsUrl) thenReturn dummyUrl

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(dummyUrl)
      }

      "redirect to contact hmrc page when both deceased flag and rls flag are true" in {
        when(mockPspDashboardService.getTiles(eqTo(pspId), any())(any()))
          .thenReturn(tiles)
        when(mockPspDashboardService.getPspDetails(eqTo(pspId))(any()))
          .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = true, deceasedFlag = true)))
        when(mockUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Json.obj()))

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ContactHMRCController.onPageLoad().url)
      }
    }

    "changeRoleToPspAndLoadPage" must {
      "redirect to onPageLoad after updating Mongo with PSP role" in {
        when(mockPspDashboardService.getTiles(eqTo(pspId), any())(any())).thenReturn(tiles)
        when(mockPspDashboardService.getPspDetails(eqTo(pspId))(any()))
          .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = false, deceasedFlag = false)))
        when(mockUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Json.obj()))
        when(mockUserAnswersCacheConnector.upsert(any(), any())(any(), any()))
          .thenReturn(Future.successful(Json.obj()))
        when(mockSessionDataCacheConnector.fetch(any())(any(), any()))
          .thenReturn(Future.successful(None))
        val jsonCaptor = ArgumentCaptor.forClass(classOf[JsValue])
        when(mockSessionDataCacheConnector.upsert(any(), jsonCaptor.capture())(any(), any()))
          .thenReturn(Future.successful(JsNull))

        val result = controller().changeRoleToPspAndLoadPage(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(PspDashboardController.onPageLoad().url)

        verify(mockSessionDataCacheConnector, times(1)).upsert(any(), any())(any(), any())
        UserAnswers(jsonCaptor.getValue).get(AdministratorOrPractitionerId) mustBe Some(Practitioner)

      }
    }
  }
}

object PspDashboardControllerSpec {
  val pspName = "Test Psp Name"
  private val pspId = "00000000"
  private val returnLink: Link =
    Link(
      id = "switch-psa",
      url = SchemesOverviewController.changeRoleToPsaAndLoadPage().url,
      linkText = Message("messages__pspDashboard__switch_psa")
    )
}




