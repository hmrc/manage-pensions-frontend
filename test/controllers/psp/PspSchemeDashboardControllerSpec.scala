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

package controllers.psp

import config.FrontendAppConfig
import connectors.{FrontendConnector, UserAnswersCacheConnector}
import connectors.admin.{FeatureToggleConnector, MinimalConnector, ToggleDetails}
import connectors.scheme.{ListOfSchemesConnector, SchemeDetailsConnector}
import controllers.ControllerSpecBase
import controllers.actions.{AuthAction, FakeAuthAction, PspSchemeAuthAction}
import handlers.ErrorHandler
import models.{IndividualDetails, Link, MinimalPSAPSP}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import play.twirl.api.Html
import services.{PspSchemeDashboardService, SchemeDetailsService}
import testhelpers.CommonBuilders.{listOfSchemesResponse, pspDetails}
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.http.{HttpResponse, SessionKeys}
import utils.UserAnswers
import viewmodels.PspSchemeDashboardCardViewModel
import views.html.psp.pspSchemeDashboard

import scala.concurrent.Future

class PspSchemeDashboardControllerSpec
  extends ControllerSpecBase
    with MockitoSugar
    with BeforeAndAfterEach {

  import PspSchemeDashboardControllerSpec._

  private val schemeDetailsService = mock[SchemeDetailsService]
  private val schemeDetailsConnector = mock[SchemeDetailsConnector]
  private val listSchemesConnector = mock[ListOfSchemesConnector]
  private val pspSchemeDashboardService = mock[PspSchemeDashboardService]
  private val userAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val appConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val minimalConnector: MinimalConnector = mock[MinimalConnector]
  private val errorHandler: ErrorHandler = mock[ErrorHandler]
  private val frontendConnector = mock[FrontendConnector]

  private val view: pspSchemeDashboard = app.injector.instanceOf[pspSchemeDashboard]
  private val mockFeatureToggleConnector = mock[FeatureToggleConnector]

  private def sessionRequest = fakeRequest.withSession(SessionKeys.sessionId-> "testSessionId")

  private def authAction(pspId: String): AuthAction =
    FakeAuthAction.createWithPspId(pspId)

  def controller(pspId: PspId = PspId("00000000")): PspSchemeDashboardController =
    new PspSchemeDashboardController(
      messagesApi = messagesApi,
      schemeDetailsConnector = schemeDetailsConnector,
      authenticate = authAction(pspId.id),
      minimalConnector = minimalConnector,
      errorHandler = errorHandler,
      listSchemesConnector = listSchemesConnector,
      userAnswersCacheConnector = userAnswersCacheConnector,
      sessionCacheConnector = userAnswersCacheConnector,
      schemeDetailsService = schemeDetailsService,
      controllerComponents = controllerComponents,
      service = pspSchemeDashboardService,
      view = view,
      config = appConfig,
      frontendConnector = frontendConnector,
      fakePspSchemeAuthAction,
      getDataWithPspName(),
      mockFeatureToggleConnector
    )

  private def practitionerCard(clientReference: Option[String]): PspSchemeDashboardCardViewModel =
    PspSchemeDashboardCardViewModel(
      id = "practitioner-card",
      heading = "Your practitioner details",
      subHeadings = Seq(
        ("Authorised by:", psaName),
        ("Date authorised to this scheme:", authDate)
      ),
      optionalSubHeading = clientReference map {
        ref =>
          ("Client reference:", ref)
      },
      links = Seq(deauthoriseLink),
      html = None
    )

  private def schemeCard(openDate: Option[String]): PspSchemeDashboardCardViewModel =
    PspSchemeDashboardCardViewModel(
      id = "scheme-card",
      heading = "Pension scheme details",
      subHeadings = Seq(("Pension Scheme Tax Reference:", pstr)),
      optionalSubHeading = openDate map {
        date =>
          ("Registration for Tax:", date)
      },
      links = Seq(searchSchemeLink),
      html = None
    )

  private def cards(
                     clientReference: Option[String],
                     openDate: Option[String]
                   ): Seq[PspSchemeDashboardCardViewModel] =
    Seq(
      practitionerCard(clientReference),
      schemeCard(openDate)
    )


  private def viewAsString(
                            clientReference: Option[String] = None,
                            openDate: Option[String] = None,
                            aftReturnsCard: Html = aftPspSchemeDashboardCards,
                            evPspCard: Html = evPspSchemeDashboardCard
                          ): String = view(
    schemeName = schemeName,
    cards = cards(clientReference, openDate),
    aftPspSchemeDashboardCards = aftReturnsCard,
    evPspSchemeDashboardCard = evPspCard,
    returnLink = Some(returnLink)
  )(
    fakeRequest,
    messages
  ).toString

  override def beforeEach(): Unit = {
    reset(schemeDetailsService)
    reset(schemeDetailsConnector)
    reset(listSchemesConnector)
    reset(pspSchemeDashboardService)
    reset(userAnswersCacheConnector)
    reset(appConfig)
    reset(minimalConnector)
    reset(errorHandler)
    reset(mockFeatureToggleConnector)
    when(userAnswersCacheConnector.removeAll(any())(any(), any()))
      .thenReturn(Future.successful(Ok("")))
    when(schemeDetailsService.retrievePspSchemeDashboardCards(any(), any(), any())(any()))
      .thenReturn(Future.successful(aftPspSchemeDashboardCards))
    when(userAnswersCacheConnector.upsert(any(), any())(any(), any()))
      .thenReturn(Future.successful(JsBoolean(true)))
    when(appConfig.pspTaskListUrl)
      .thenReturn("/foo")
    when(mockFeatureToggleConnector.getNewAftFeatureToggle(any())(any())).thenReturn(Future.successful(ToggleDetails("test", Some("test"), false)))
  }

  "PspSchemeDashboardController.onPageLoad" must {
    "return ok and correct cards when start aft is allowed" in {
      when(frontendConnector.retrieveEventReportingPartial(any(), any()))
        .thenReturn(Future.successful(evPspSchemeDashboardCard))
      when(schemeDetailsConnector.getPspSchemeDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(ua()))
      when(minimalConnector.getMinimalPspDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = false, deceasedFlag = false)))
      when(listSchemesConnector.getListOfSchemesForPsp(any())(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemesResponse)))
      when(pspSchemeDashboardService.getTiles(any(), any(), any(), any(), any())(any()))
        .thenReturn(cards(None, None))
      when(appConfig.pspSchemeDashboardUrl).thenReturn("dummyUrl")


      val result = controller().onPageLoad(srn)(sessionRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return ok and correct cards when start aft is not allowed" in {
      when(frontendConnector.retrieveEventReportingPartial(any(), any()))
        .thenReturn(Future.successful(evPspSchemeDashboardCard))
      when(schemeDetailsConnector.getPspSchemeDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(ua("pending")))
      when(minimalConnector.getMinimalPspDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = false, deceasedFlag = false)))
      when(listSchemesConnector.getListOfSchemesForPsp(any())(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemesResponse)))
      when(pspSchemeDashboardService.getTiles(any(), any(), any(), any(), any())(any()))
        .thenReturn(cards(None, None))
      when(appConfig.pspSchemeDashboardUrl).thenReturn("dummyUrl")


      val result = controller().onPageLoad(srn)(sessionRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(aftReturnsCard = Html(""))
    }

    "return ok and correct cards when open date and client ref are populated" in {
      when(frontendConnector.retrieveEventReportingPartial(any(), any()))
        .thenReturn(Future.successful(evPspSchemeDashboardCard))
      when(schemeDetailsConnector.getPspSchemeDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(ua()))
      when(minimalConnector.getMinimalPspDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = false, deceasedFlag = false)))
      when(listSchemesConnector.getListOfSchemesForPsp(any())(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemesResponse)))
      when(pspSchemeDashboardService.getTiles(any(), any(), any(), any(), any())(any()))
        .thenReturn(cards(Some(clientRef), Some(authDate)))
      when(appConfig.pspSchemeDashboardUrl).thenReturn("dummyUrl")

      val result = controller().onPageLoad(srn)(sessionRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(Some(clientRef), Some(authDate))
    }

    "return not found when list schemes does not come back" in {
      when(frontendConnector.retrieveEventReportingPartial(any(), any()))
        .thenReturn(Future.successful(evPspSchemeDashboardCard))
      when(schemeDetailsConnector.getPspSchemeDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(ua()))
      when(minimalConnector.getMinimalPspDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = false, deceasedFlag = false)))
      when(listSchemesConnector.getListOfSchemesForPsp(any())(any(), any()))
        .thenReturn(Future.successful(Left(HttpResponse(404, "Not found"))))
      when(errorHandler.notFoundTemplate(any()))
        .thenReturn(Html(""))

      val result = controller().onPageLoad(srn)(fakeRequest)

      status(result) mustBe NOT_FOUND
    }

    "return redirect to update contact details page when RLS flag true" in {
      when(schemeDetailsConnector.getPspSchemeDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(ua()))
      when(minimalConnector.getMinimalPspDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = true, deceasedFlag = false)))
      when(appConfig.pspUpdateContactDetailsUrl)
        .thenReturn("/update-contact-details")

      val result = controller().onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(appConfig.pspUpdateContactDetailsUrl)
    }

    "return redirect to contact hmrc page when both RLS flag and deceased flag are true" in {
      when(schemeDetailsConnector.getPspSchemeDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(ua()))
      when(minimalConnector.getMinimalPspDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = true, deceasedFlag = true)))

      val result = controller().onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ContactHMRCController.onPageLoad().url)
    }

    "return not found if psp is not authorised" in {

      def controller(pspId: PspId) = new PspSchemeDashboardController(
        messagesApi = messagesApi,
        schemeDetailsConnector = schemeDetailsConnector,
        authenticate = authAction(pspId.id),
        minimalConnector = minimalConnector,
        errorHandler = errorHandler,
        listSchemesConnector = listSchemesConnector,
        userAnswersCacheConnector = userAnswersCacheConnector,
        sessionCacheConnector = userAnswersCacheConnector,
        schemeDetailsService = schemeDetailsService,
        controllerComponents = controllerComponents,
        service = pspSchemeDashboardService,
        view = view,
        config = appConfig,
        frontendConnector = frontendConnector,
        app.injector.instanceOf[PspSchemeAuthAction],
        getDataWithPspName(),
        mockFeatureToggleConnector
      )

      when(schemeDetailsConnector.getPspSchemeDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(ua()))
      when(minimalConnector.getMinimalPspDetails(any())(any(), any()))
        .thenReturn(Future.successful(minimalPsaDetails(rlsFlag = false, deceasedFlag = false)))


      val result = controller(PspId("00000001")).onPageLoad(srn)(fakeRequest)

      status(result) mustBe NOT_FOUND
    }
  }
}

object PspSchemeDashboardControllerSpec {

  private val psaName = "Test Psa Name"
  private val schemeName = "Test Scheme"
  private val pstr = "Test pstr"
  private val srn = "S1000000456"
  private val authDate = "2020-01-01"
  private val clientRef = "123"

  private def minimalPsaDetails(rlsFlag: Boolean, deceasedFlag: Boolean): MinimalPSAPSP =
    MinimalPSAPSP(
      email = "test@test.com",
      isPsaSuspended = false,
      organisationName = None,
      individualDetails = Some(IndividualDetails("Test", None, "Psp Name")),
      rlsFlag = rlsFlag,
      deceasedFlag = deceasedFlag
    )

  private def ua(schemeStatus: String = "open"): UserAnswers =
    UserAnswers(
      Json.obj(
        "pspDetails" -> pspDetails,
        "schemeName" -> schemeName,
        "pstr" -> pstr,
        "schemeStatus" -> schemeStatus
      )
    )

  private val returnLink: Link =
    Link(
      id = "return-search-schemes",
      url = controllers.psp.routes.ListSchemesController.onPageLoad.url,
      linkText = "Return to your pension schemes"
    )
  private val deauthoriseLink: Link =
    Link(
      id = "deauthorise-yourself",
      url = controllers.psp.deauthorise.self.routes.ConfirmDeauthController.onPageLoad().url,
      linkText = "De Authorise yourself"
    )
  private val searchSchemeLink: Link =
    Link(
      id = "return-search-schemes",
      url = "/foo",
      linkText = "View the registered scheme details"
    )
  private val aftPspSchemeDashboardCards = Html("psp-scheme-dashboard-cards-html")
  private val evPspSchemeDashboardCard = Html("ev-scheme-dashboard-cards-html")
}
