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

package controllers

import config._
import connectors.{SessionDataCacheConnector, UserAnswersCacheConnector}
import controllers.actions._
import controllers.psa.routes.ListSchemesController
import identifiers.AdministratorOrPractitionerId
import models.AdministratorOrPractitioner.Administrator
import models.{Link, MinimalPSAPSP}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.test.Helpers.{contentAsString, _}
import play.twirl.api.Html
import services.SchemesOverviewService
import utils.UserAnswers
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}
import views.html.schemesOverview

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.Future

class SchemesOverviewControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  import SchemesOverviewControllerSpec._

  val subHeading: String = Message("messages__psaDashboard__sub_heading")
  val fakeSchemesOverviewService: SchemesOverviewService = mock[SchemesOverviewService]
  val fakeUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  val appConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val mockSessionDataCacheConnector: SessionDataCacheConnector = mock[SessionDataCacheConnector]

  private val view: schemesOverview = app.injector.instanceOf[schemesOverview]

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData): SchemesOverviewController =
    new SchemesOverviewController(messagesApi, fakeSchemesOverviewService, FakeAuthAction,
      dataRetrievalAction, fakeUserAnswersCacheConnector, mockSessionDataCacheConnector, controllerComponents, appConfig, view)

  def viewAsString(): String = view(
    name = psaName,
    title = "site.psa",
    schemeTile,
    adminTile,
    penaltiesCardHtml = Some(html),
    migrationHtml = Some(html),
    subHeading = Some(subHeading),
    returnLink = None
  )(fakeRequest, messages).toString

  private def minimalDetails(rlsFlag: Boolean = false, deceasedFlag: Boolean = false) = MinimalPSAPSP(
    email = "a@a.c",
    isPsaSuspended = false,
    organisationName = None,
    individualDetails = None,
    rlsFlag = rlsFlag,
    deceasedFlag = deceasedFlag
  )

  private val dummyURl = "/url"

  "SchemesOverview Controller" when {
    "onPageLoad" must {
      "return OK and the correct tiles" in {
        when(fakeSchemesOverviewService.getTiles(eqTo(psaId))(any(), any(), any())).thenReturn(Future.successful(Seq(schemeCard, adminCard)))
        when(fakeSchemesOverviewService.getPsaName(eqTo(psaId))(any()))
          .thenReturn(Future.successful(Some(psaName)))
        when(fakeSchemesOverviewService.getPsaMinimalDetails(any())(any()))
          .thenReturn(Future.successful(minimalDetails()))
        when(fakeSchemesOverviewService.retrievePenaltiesUrlPartial(any(), any()))
          .thenReturn(Future.successful(html))
        when(fakeSchemesOverviewService.retrieveMigrationTile(any(), any()))
          .thenReturn(Future.successful(Some(html)))
        when(fakeUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(Json.obj()))

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to update contact address when RLS flag is set" in {
        when(fakeSchemesOverviewService.getTiles(eqTo(psaId))(any(), any(), any())).thenReturn(Future.successful(Seq(schemeCard, adminCard)))
        when(fakeSchemesOverviewService.getPsaName(eqTo(psaId))(any()))
          .thenReturn(Future.successful(Some(psaName)))
        when(fakeSchemesOverviewService.getPsaMinimalDetails(any())(any()))
          .thenReturn(Future.successful(minimalDetails(rlsFlag = true)))
        when(fakeUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(Json.obj()))
        when(appConfig.psaUpdateContactDetailsUrl).thenReturn(dummyURl)

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.psaUpdateContactDetailsUrl)
      }

      "redirect to contact HMRC page when both rls and deceased flag are set" in {
        when(fakeSchemesOverviewService.getTiles(eqTo(psaId))(any(), any(), any())).thenReturn(Future.successful(Seq(schemeCard, adminCard)))
        when(fakeSchemesOverviewService.getPsaName(eqTo(psaId))(any()))
          .thenReturn(Future.successful(Some(psaName)))
        when(fakeSchemesOverviewService.getPsaMinimalDetails(any())(any()))
          .thenReturn(Future.successful(minimalDetails(rlsFlag = true, deceasedFlag = true)))
        when(fakeUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(Json.obj()))
        when(appConfig.psaUpdateContactDetailsUrl).thenReturn(dummyURl)

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ContactHMRCController.onPageLoad().url)
      }


    }
    "changeRoleToPsaAndLoadPage" must {
      "redirect to overview page and update mongo" in {
        when(fakeSchemesOverviewService.getTiles(eqTo(psaId))(any(), any(), any())).thenReturn(Future.successful(Seq(schemeCard, adminCard)))
        when(fakeSchemesOverviewService.getPsaName(eqTo(psaId))(any()))
          .thenReturn(Future.successful(Some(psaName)))
        when(fakeSchemesOverviewService.getPsaMinimalDetails(any())(any()))
          .thenReturn(Future.successful(minimalDetails()))
        when(fakeSchemesOverviewService.retrievePenaltiesUrlPartial(any(), any()))
          .thenReturn(Future.successful(html))
        when(fakeUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(Json.obj()))
        when(fakeUserAnswersCacheConnector.upsert(any(), any())(any(), any()))
          .thenReturn(Future.successful(Json.obj()))
        when(mockSessionDataCacheConnector.fetch(any())(any(), any()))
          .thenReturn(Future.successful(None))
        val jsonCaptor = ArgumentCaptor.forClass(classOf[JsValue])
        when(mockSessionDataCacheConnector.upsert(any(), jsonCaptor.capture())(any(), any()))
          .thenReturn(Future.successful(JsNull))

        val result = controller().changeRoleToPsaAndLoadPage(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.SchemesOverviewController.onPageLoad().url)
        verify(mockSessionDataCacheConnector, times(1)).upsert(any(), any())(any(), any())
        UserAnswers(jsonCaptor.getValue).get(AdministratorOrPractitionerId) mustBe Some(Administrator)
      }
    }
    "onRedirect" must {

      "redirect to overview page" in {

        val result = controller().redirect(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.SchemesOverviewController.onPageLoad().url
      }
    }
  }
}

object SchemesOverviewControllerSpec extends ControllerSpecBase {
  val schemeName = "Test Scheme Name"
  val psaName = "Test Psa Name"
//  private val formatter = DateTimeFormatter.ofPattern("dd MMMM YYYY")
  private val psaId = "A0000000"
  val html: Html = Html("test-html")
  val deleteDate: String = LocalDate.now(ZoneOffset.UTC).plusDays(frontendAppConfig.daysDataSaved).toString

  private val adminCard = CardViewModel(
    id = "administrator-card",
    heading = Message("messages__schemeOverview__psa_heading"),
    subHeadings = Seq(CardSubHeading(
      subHeading = Message("messages__schemeOverview__psa_id"),
      subHeadingClasses = "heading-small card-sub-heading",
      subHeadingParams = Seq(CardSubHeadingParam(
        subHeadingParam = psaId,
        subHeadingParamClasses = "font-small")))),
    links = Seq(
      Link("psaLink", frontendAppConfig.registeredPsaDetailsUrl, Message("messages__schemeOverview__psa_change")),
      Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
        Message("messages__schemeOverview__psa_view_invitations")
      ),
      Link("deregister-link", frontendAppConfig.psaDeregisterUrl,
        Message("messages__schemeOverview__psa_deregister"))
    ))

  private val schemeCard = CardViewModel(
    id = "scheme-card",
    heading = Message("messages__schemeOverview__scheme_heading"),
    links = Seq(
      Link("view-schemes", ListSchemesController.onPageLoad.url, Message("messages__schemeOverview__scheme_view"))
    ),
    html = Some(html)
  )

  private val adminTile = adminCard
  private val schemeTile = schemeCard
}




