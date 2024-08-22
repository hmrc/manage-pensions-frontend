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

package controllers.psa

import config.FrontendAppConfig
import connectors.admin.MinimalConnector
import connectors.scheme.{ListOfSchemesConnector, PensionSchemeVarianceLockConnector, SchemeDetailsConnector}
import connectors.{FakeUserAnswersCacheConnector, FrontendConnector}
import controllers.ControllerSpecBase
import controllers.actions.FakeAuthAction
import controllers.invitations.psp.routes.WhatYouWillNeedController
import controllers.invitations.routes.InviteController
import controllers.psa.routes.ViewAdministratorsController
import controllers.psp.routes.ViewPractitionersController
import identifiers.invitations.PSTRId
import identifiers.{SchemeNameId, SchemeStatusId}
import models.SchemeStatus.{Open, Rejected}
import models._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers.{contentAsString, _}
import play.twirl.api.Html
import services.PsaSchemeDashboardService
import uk.gov.hmrc.http.SessionKeys
import utils.DateHelper.formatter
import utils.UserAnswers
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}
import views.html.psa.psaSchemeDashboard

import java.time.LocalDate
import scala.concurrent.Future

class PsaSchemeDashboardControllerSpec extends ControllerSpecBase
    with MockitoSugar with BeforeAndAfterEach {

  private val psaSchemeDashboardView: psaSchemeDashboard = app.injector.instanceOf[psaSchemeDashboard]
  private val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val mockFrontendConnector: FrontendConnector = mock[FrontendConnector]
  private val fakeListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val fakeSchemeLockConnector: PensionSchemeVarianceLockConnector = mock[PensionSchemeVarianceLockConnector]
  private val mockService: PsaSchemeDashboardService = mock[PsaSchemeDashboardService]

  private val schemeName = "Benefits Scheme"
  private val aftHtml = Html("test-aft-html")
  private val aftEmptyHtml = Html("")
  private val finInfoHtml = Html("test-fininfo-html")
  private val erHtml = Html("test-er-html")

  private val mockMinimalPsaConnector: MinimalConnector =
    mock[MinimalConnector]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val name = "test-name"
  private val date = "2020-01-01"
  private val windUpDate = "2020-02-01"
  private val pstr = "pstr"
  private val listOfSchemes: ListOfSchemes = ListOfSchemes("", "", Some(List(SchemeDetails(name, srn, "Open", Some(date), Some(windUpDate), Some(pstr), None))))

  private def sessionRequest = fakeRequest.withSession(SessionKeys.sessionId -> "testSessionId")

  private def psaCard(inviteLink: Seq[Link] = inviteLink)
                     (implicit messages: Messages): CardViewModel = CardViewModel(
    id = "psa_list",
    heading = Message("messages__psaSchemeDash__psa_list_head"),
    subHeadings = Seq(CardSubHeading(
      subHeading = messages("messages__psaSchemeDash__addedOn", LocalDate.parse("2018-07-01").format(formatter)),
      subHeadingClasses = "card-sub-heading",
      subHeadingParams = Seq(CardSubHeadingParam(
        subHeadingParam = "Tony A Smith",
        subHeadingParamClasses = "font-small bold")))),
    links = inviteLink ++ Seq(
      Link("view-psa-list",
        ViewAdministratorsController.onPageLoad(srn).url,
        Message("messages__psaSchemeDash__view_psa"))
    )
  )

  private def schemeCard(linkText: String = "messages__psaSchemeDash__view_details_link")(implicit messages: Messages): CardViewModel = CardViewModel(
    id = "scheme_details",
    heading = Message("messages__psaSchemeDash__scheme_details_head"),
    subHeadings = pstrSubHead ++ dateSubHead,
    links = Seq(Link("view-details", dummyUrl, messages(linkText)))
  )

  private def pspCard()(implicit messages: Messages): CardViewModel = CardViewModel(
    id = "psp_list",
    heading = Message("messages__psaSchemeDash__psp_heading"),
    subHeadings = Seq(CardSubHeading(
      subHeading = Message("messages__psaSchemeDash__addedOn", LocalDate.parse("2019-02-01").format(formatter)),
      subHeadingClasses = "card-sub-heading",
      subHeadingParams = Seq(CardSubHeadingParam(
        subHeadingParam = "Practitioner Individual",
        subHeadingParamClasses = "font-small bold")))),
    links = Seq(
      Link("authorise", WhatYouWillNeedController.onPageLoad(srn).url,
        Message("messages__pspAuthorise__link")),
      Link("view-practitioners", ViewPractitionersController.onPageLoad(srn).url,
        linkText = Message("messages__pspViewOrDeauthorise__link")
      ))
  )

  private def pstrSubHead(implicit messages: Messages): Seq[CardSubHeading] = Seq(CardSubHeading(
    subHeading = Message("messages__psaSchemeDash__pstr"),
    subHeadingClasses = "card-sub-heading",
    subHeadingParams = Seq(CardSubHeadingParam(
      subHeadingParam = pstr,
      subHeadingParamClasses = "font-small bold"))))

  private def dateSubHead(implicit messages: Messages): Seq[CardSubHeading] = Seq(CardSubHeading(
    subHeading = Message("messages__psaSchemeDash__regDate"),
    subHeadingClasses = "card-sub-heading",
    subHeadingParams = Seq(CardSubHeadingParam(
      subHeadingParam = LocalDate.parse(date).format(formatter),
      subHeadingParamClasses = "font-small bold"))))

  private def inviteLink = Seq(Link(
    id = "invite",
    url = InviteController.onPageLoad(srn).url,
    linkText = Message("messages__psaSchemeDash__invite_link")
  ))

  private def controller(): PsaSchemeDashboardController = {
    new PsaSchemeDashboardController(
      messagesApi,
      fakeSchemeDetailsConnector,
      fakeListOfSchemesConnector,
      fakeSchemeLockConnector,
      FakeAuthAction,
      FakeUserAnswersCacheConnector,
      FakeUserAnswersCacheConnector,
      controllerComponents,
      mockService,
      psaSchemeDashboardView,
      mockFrontendConnector,
      mockMinimalPsaConnector,
      mockAppConfig,
      fakePsaSchemeAuthAction,
      getDataWithPsaName()
    )
  }

  private def userAnswers(schemeStatus: String): UserAnswers = UserAnswers(Json.obj(
    PSTRId.toString -> pstr,
    "schemeStatus" -> schemeStatus,
    SchemeNameId.toString -> schemeName,
    "psaDetails" -> JsArray(
      Seq(
        Json.obj(
          "id" -> "A0000000",
          "organisationOrPartnershipName" -> "partnership name 2",
          "relationshipDate" -> "2018-07-01"
        ),
        Json.obj(
          "id" -> "A0000001",
          "individual" -> Json.obj(
            "firstName" -> "Tony",
            "middleName" -> "A",
            "lastName" -> "Smith"
          ),
          "relationshipDate" -> "2018-07-01"
        )
      )
    ),
    "pspDetails" -> JsArray(
      Seq(
        Json.obj(
          "id" -> "A0000000",
          "organisationOrPartnershipName" -> "Practitioner Organisation",
          "relationshipStartDate" -> "2019-02-01",
          "authorisingPSAID" -> "123",
          "authorisingPSA" -> Json.obj(
            "organisationOrPartnershipName" -> "Tony A Smith"
          )
        ),
        Json.obj(
          "id" -> "A0000001",
          "individual" -> Json.obj(
            "firstName" -> "Practitioner",
            "lastName" -> "Individual"
          ),
          "relationshipStartDate" -> "2019-02-01",
          "authorisingPSAID" -> "123",
          "authorisingPSA" -> Json.obj(
            "organisationOrPartnershipName" -> "Tony A Smith"
          )
        )
      )
    )
  ))

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
    reset(fakeSchemeDetailsConnector)
    reset(fakeListOfSchemesConnector)
    reset(fakeSchemeLockConnector)
    reset(mockService)
    when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(eqTo("A0000000"), any())(any(), any()))
      .thenReturn(Future.successful(Some(VarianceLock)))
  }

  "PsaSchemeDashboardController" must {
    "return OK and the correct view for a GET and NO financial info html if status is NOT open" in {
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      val ua = userAnswers(Open.value).set(SchemeStatusId)(Rejected.value).asOpt.get
      val currentScheme = listOfSchemes.schemeDetails.flatMap(_.find(_.referenceNumber.contains(srn)))
      val schemeLink = Link(id = "view-details", url = dummyUrl, linkText = Message("messages__psaSchemeDash__view_details_link"))
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(ua))
      when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))
      when(mockService.cards(any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Seq(schemeCard(), psaCard(), pspCard())))
      when(mockFrontendConnector.retrieveEventReportingPartial(any(), any())).thenReturn(Future(erHtml))
      when(mockAppConfig.psaSchemeDashboardUrl).thenReturn(dummyUrl)
      when(mockService.optionLockedSchemeName(any())(any())).thenReturn(Future.successful(None))
      val result = controller().onPageLoad(srn)(sessionRequest)
      status(result) mustBe OK

      val expected = psaSchemeDashboardView(schemeName, false, currentScheme, "Rejected", schemeLink, aftHtml = Html(""), finInfoHtml = Html(""), erHtml,
        Seq(schemeCard(), psaCard(), pspCard()))(sessionRequest, messages).toString()
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
      val currentScheme = listOfSchemes.schemeDetails.flatMap(_.find(_.referenceNumber.contains(srn)))
      val schemeLink = Link("view-details", dummyUrl, messages("messages__psaSchemeDash__view_details_link"))
      when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPSAPSP()))
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(userAnswers(Open.value)))
      when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))
      when(mockService.cards(any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Seq(schemeCard(), psaCard(), pspCard())))
      when(mockFrontendConnector.retrieveAftPartial(any())(any(), any())).thenReturn(Future(aftHtml))
      when(mockFrontendConnector.retrieveFinInfoPartial(any())(any(), any())).thenReturn(Future(finInfoHtml))
      when(mockFrontendConnector.retrieveEventReportingPartial(any(), any())).thenReturn(Future(erHtml))
      when(mockService.optionLockedSchemeName(any())(any())).thenReturn(Future.successful(None))
      val result = controller().onPageLoad(srn)(sessionRequest)
      status(result) mustBe OK

      val expected = psaSchemeDashboardView(schemeName, false, currentScheme, "Open", schemeLink, aftHtml = aftHtml, finInfoHtml = finInfoHtml, erHtml,
        Seq(schemeCard(), psaCard(), pspCard()))(sessionRequest, messages).toString()
      contentAsString(result) mustBe expected
    }

  }
}
