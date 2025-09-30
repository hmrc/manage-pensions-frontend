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

package services

import base.SpecBase
import config.FrontendAppConfig
import connectors.PensionSchemeReturnConnector
import connectors.scheme.{PensionSchemeVarianceLockConnector, SchemeDetailsConnector}
import controllers.invitations.psp.routes._
import controllers.invitations.routes._
import controllers.psa.routes._
import controllers.psp.routes._
import identifiers.invitations.PSTRId
import identifiers.{SchemeNameId, SchemeStatusId}
import models.AuthEntity.PSA
import models.SchemeStatus.{Open, Pending, Rejected}
import models._
import models.requests.AuthenticatedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.AnyContent
import play.twirl.api.Html
import services.SchemeDetailsServiceSpec.psaId
import uk.gov.hmrc.domain.PsaId
import utils.DateHelper.formatter
import utils.UserAnswers
import viewmodels.Message.Literal
import viewmodels._

import java.time.LocalDate
import scala.concurrent.Future

class PsaSchemeDashboardServiceSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaFutures {

  import PsaSchemeDashboardServiceSpec._
  implicit val authReq: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(fakeRequest, "id", Some(PsaId(psaId)), None, Individual, PSA)

  private val mockAppConfig = mock[FrontendAppConfig]
  private val mockPensionSchemeVarianceLockConnector = mock[PensionSchemeVarianceLockConnector]
  private val mockSchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val mockEventReportingConnector = mock[PensionSchemeReturnConnector]
  def listOfSchemes: ListOfSchemes = ListOfSchemes("", "", Some(fullSchemes))

  def fullSchemes: List[SchemeDetails] =
    List(
      SchemeDetails(
        name = "scheme-1",
        referenceNumber = "srn-1",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        windUpDate = None,
        pstr = Some("24000001IN"),
        relationship = None,
        underAppeal = None
      ),
      SchemeDetails(
        name = "scheme-2",
        referenceNumber = "S2400000005",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        windUpDate = None,
        pstr = Some("pstr-1"),
        relationship = None,
        underAppeal = None
      )
    )

  val overview1 = EROverview(
    LocalDate.of(2022, 4, 6),
    LocalDate.of(2023, 4, 5),
      Some(LocalDate.of(2024, 4, 6)),
      Some(LocalDate.of(2024, 4, 6)),
      Some("PSA"))
  private def service: PsaSchemeDashboardService =
    new PsaSchemeDashboardService(mockAppConfig, mockPensionSchemeVarianceLockConnector, mockSchemeDetailsConnector, mockEventReportingConnector)

  override def beforeEach(): Unit = {
    reset(mockAppConfig)
    reset(mockPensionSchemeVarianceLockConnector)
    reset(mockSchemeDetailsConnector)
    reset(mockEventReportingConnector)
    when(mockAppConfig.viewSchemeDetailsUrl).thenReturn(dummyUrl)
    when(mockAppConfig.psrOverviewUrl).thenReturn(dummyUrl)
    when(mockAppConfig.aftOverviewHtmlUrl).thenReturn(dummyUrl)
    when(mockAppConfig.eventReportingOverviewHtmlUrl).thenReturn(dummyUrl)
    when(mockPensionSchemeVarianceLockConnector.getLockByPsa(any())(using any(), any())).thenReturn(Future.successful(None))
    when(mockSchemeDetailsConnector.getSchemeDetails(any(), any(), any())(using any(), any())).thenReturn(Future.successful(UserAnswers()))
    when(mockEventReportingConnector.getOverview(any(), any(), any(), any(), any())(using any())).thenReturn(Future.successful(Seq(overview1)))
    super.beforeEach()
  }

  "schemeCard" must {
    "return model fron aft-frontend is Scheme status is open and psa holds the lock" in {
      service.schemeCard(srn, currentScheme(Open), Some(VarianceLock), userAnswers(Open.value), None) mustBe
        schemeCard("messages__psaSchemeDash__view_change_details_link")
    }

    "return model with view-only link for scheme if psa does not hold lock" in {
      service.schemeCard(srn, currentScheme(Open), Some(SchemeLock), userAnswers(Open.value), None) mustBe
        schemeCard(notificationText = Some(Message("messages__psaSchemeDash__view_change_details_link_notification_scheme", "<strong>" + name + "</strong>")))
    }

    "return model with view-only link for scheme if psa does hold lock where scheme name not returned" in {
      service.schemeCard(srn, currentScheme(Open), Some(PsaLock), userAnswers(Open.value), None) mustBe
        schemeCard(notificationText = Some(Message("messages__psaSchemeDash__view_change_details_link_notification_psa-unknown_scheme")))
    }

    "return model with view-only link for scheme if psa does hold lock where scheme name returned for locked scheme" in {
      service.schemeCard(srn, currentScheme(Open), Some(PsaLock), userAnswers(Open.value), Some(anotherSchemeName)) mustBe
        schemeCard(notificationText = Some(Message("messages__psaSchemeDash__view_change_details_link_notification_psa",
          "<strong>" + anotherSchemeName + "</strong>")))
    }

    "return not display subheadings if scheme is not open" in {
      val ua = UserAnswers().set(SchemeStatusId)(Rejected.value).asOpt.get

      service.schemeCard(srn, currentScheme(Open), Some(SchemeLock), ua, None) mustBe closedSchemeCard()
    }
  }

  "manageReportsEventsCard" must {
    "return manage reports events card view model" in {
      service.manageReportsEventsCard(srn, erHtml, "") mustBe
        manageReportsEventsCard(erHtml, "")
    }
  }

  "psaCard" must {
    "return model when scheme is open" in {
      service.psaCard(srn, userAnswers(Open.value)) mustBe psaCard()
    }

    "return model when scheme is not open" in {
      val ua = userAnswers(Rejected.value)
      service.psaCard(srn, ua) mustBe psaCard(Nil)
    }
  }

  "psaCardInterimDashboard" must {
    "return model when scheme is open" in {
      service.psaCardForInterimDashboard(srn, userAnswers(Open.value)) mustBe psaCardForInterimDashboard()
    }

    "return model when scheme is not open" in {
      val ua = userAnswers(Rejected.value)
      service.psaCardForInterimDashboard(srn, ua) mustBe psaCardForInterimDashboard(Nil)
    }

    "return model when relationship date does not exist" in {
      val ua = userAnswers(Pending.value, psaDetailsWithNoDate)
      service.psaCardForInterimDashboard(srn, ua) mustBe psaCardForInterimDashboard(Nil).copy(subHeadings = Nil )
    }
  }

  "pspCard" must {
    "return model when psps are present and scheme status is open" in {
      service.pspCard(userAnswers(Open.value), Some(Open.value), srn) mustBe List(pspCard())
    }

    "return empty list when psps are present and scheme status is not open" in {
      service.pspCard(userAnswers(Rejected.value), Some(Rejected.value), srn) mustBe Nil
    }
  }


  "PsaSchemeDashboardService" must {

    "handle different no seqEROverview correctly" in {
      val mockEventReportingConnector = mock[PensionSchemeReturnConnector]
      when(mockEventReportingConnector.getOverview(any(), any(), any(), any(), any())(using any())).thenReturn(Future.successful(Seq.empty))

      val service = new PsaSchemeDashboardService(mockAppConfig, mockPensionSchemeVarianceLockConnector, mockSchemeDetailsConnector, mockEventReportingConnector)

      val actualReturn = service.cards(showPsrLink = true, erHtml = Html(""), srn = "S2400000005", lock = None, list =
        ListOfSchemes("", "", Some(fullSchemes)), ua = UserAnswers()) .map(_.head).futureValue

      val expectedReturn = CardViewModel("manage_reports_returns","Manage reports and returns",List.empty,
        List(Link("aft-view-link","dummy",Literal("Accounting for Tax (AFT) return"),None,None)),None)

      compareCardViewModels(actualReturn, expectedReturn)


    }
    "handle different one seqEROverview correctly" in {
      val mockEventReportingConnector = mock[PensionSchemeReturnConnector]
      when(mockEventReportingConnector.getOverview(any(), any(), any(), any(), any())(using any())).thenReturn(Future.successful(Seq(overview1)))

      val service = new PsaSchemeDashboardService(mockAppConfig, mockPensionSchemeVarianceLockConnector, mockSchemeDetailsConnector, mockEventReportingConnector)

      val actualReturn = service.cards(showPsrLink = true, erHtml = Html(""), srn = "S2400000005", lock = None, list =
        ListOfSchemes("", "", Some(fullSchemes)), ua = UserAnswers()) .map(_.head).futureValue

      val expectedReturn = CardViewModel(
        "manage_reports_returns",
        "Manage reports and returns",
        List(
          CardSubHeading("Notice to file:", "card-sub-heading",
            List(CardSubHeadingParam("PSR due 6 April 2024", "font-small bold"))
          )
        ),
        List(
          Link("aft-view-link", "dummy", Literal("Accounting for Tax (AFT) return"), None, None),
          Link("psr-view-details", "dummy", Literal("Pension scheme return"), None, None)
        ) ++ (
          if (mockAppConfig.enableQROPSUrl) {
            List(
              Link("qrops-view-details", "dummy",
                Literal("Report a transfer to a qualified recognised overseas pension scheme"),
                None, None
              )
            )
          } else Nil
          ),
        None
      )

      compareCardViewModels(actualReturn, expectedReturn)
    }

    "handle different multiple seqEROverview correctly" in {
      val mockEventReportingConnector = mock[PensionSchemeReturnConnector]
      when(mockEventReportingConnector.getOverview(any(), any(), any(), any(), any())(using any())).thenReturn(Future.successful(Seq(overview1, overview1)))

      val service = new PsaSchemeDashboardService(mockAppConfig, mockPensionSchemeVarianceLockConnector, mockSchemeDetailsConnector, mockEventReportingConnector)

      val actualReturn = service.cards(showPsrLink = true, erHtml = Html(""), srn = "S2400000005", lock = None, list =
        ListOfSchemes("", "", Some(fullSchemes)), ua = UserAnswers()) .map(_.head).futureValue

      val expectedReturn = CardViewModel(
        "manage_reports_returns",
        "Manage reports and returns",
        List(CardSubHeading("Notice to file:", "card-sub-heading",
          List(CardSubHeadingParam("Multiple pension scheme returns due", "font-small bold"))
        )),
        List(
          Link("aft-view-link", "dummy", Literal("Accounting for Tax (AFT) return"), None, None),
          Link("psr-view-details", "dummy", Literal("Pension scheme return"), None, None)
        ) ++ (
          if (mockAppConfig.enableQROPSUrl) {
            List(
              Link("qrops-view-details", "dummy",
                Literal("Report a transfer to a qualified recognised overseas pension scheme"),
                None, None
              )
            )
          } else Nil
          ),
        None
      )
      compareCardViewModels(actualReturn, expectedReturn)
    }

    "throw an exception if the scheme details cannot be returned" in {
      val mockEventReportingConnector = mock[PensionSchemeReturnConnector]
      when(mockEventReportingConnector.getOverview(any(), any(), any(), any(), any())(using any())).thenReturn(Future.successful(Seq(overview1, overview1)))

      val service = new PsaSchemeDashboardService(mockAppConfig, mockPensionSchemeVarianceLockConnector, mockSchemeDetailsConnector, mockEventReportingConnector)

      val exception = intercept[SchemeNotFoundException] {
        service.cards(showPsrLink = true, erHtml = Html(""), srn = "srn-2", lock = None, list =
          ListOfSchemes("", "", Some(fullSchemes)), ua = UserAnswers()) .map(_.head).futureValue
      }

      exception.message mustBe "Scheme not found - could not find scheme with reference number to match srn"
    }
  }

  def compareCardViewModels(card1: CardViewModel, card2: CardViewModel): Unit = {
    assert(card1.id == card2.id, s"Different id: ${card1.id} vs ${card2.id}")

    assert(card1.subHeadings == card2.subHeadings, s"Different subHeadings: ${card1.subHeadings} vs ${card2.subHeadings}")
    if (card1.subHeadings != card2.subHeadings) {
      card1.subHeadings.zip(card2.subHeadings).zipWithIndex.foreach { case ((sh1, sh2), idx) =>
        assert(sh1 == sh2, s"Different subHeading at index $idx: $sh1 vs $sh2")
      }
    }

    assert(card1.links == card2.links, s"Different links: ${card1.links} vs ${card2.links}")
    if (card1.links != card2.links) {
      card1.links.zip(card2.links).zipWithIndex.foreach { case ((link1, link2), idx) =>
        assert(link1 == link2, s"Different link at index $idx: $link1 vs $link2")
      }
    }

  }



}

object PsaSchemeDashboardServiceSpec {
  val srn: SchemeReferenceNumber = SchemeReferenceNumber("AB123456C")
  private val pstr = "pstr"
  private val schemeName = "Benefits Scheme"
  private val anotherSchemeName = "Another scheme"
  private val name = "test-name"
  private val date = "2020-01-01"
  private val windUpDate = "2020-02-01"
  private val dummyUrl = "dummy"
  private val erHtml = Html("")


  private val psaDetails: JsArray =  JsArray(
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
  )
  private val psaDetailsWithNoDate: JsArray =  JsArray(
    Seq(
      Json.obj(
        "id" -> "A0000000",
        "organisationOrPartnershipName" -> "partnership name 2"
      ),
      Json.obj(
        "id" -> "A0000001",
        "individual" -> Json.obj(
          "firstName" -> "Tony",
          "middleName" -> "A",
          "lastName" -> "Smith"
        )
      )
    )
  )
  private def userAnswers(schemeStatus: String, psaDetailsToUse: JsArray = psaDetails): UserAnswers = UserAnswers(Json.obj(
    PSTRId.toString -> pstr,
    "schemeStatus" -> schemeStatus,
    SchemeNameId.toString -> schemeName,
    "psaDetails" -> psaDetailsToUse,
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

  private def schemeCard(linkText: String = "messages__psaSchemeDash__view_details_link",
                         notificationText: Option[Message] = None)(implicit messages: Messages): CardViewModel = CardViewModel(
    id = "scheme_details",
    heading = Message("messages__psaSchemeDash__scheme_details_head"),
    subHeadings = pstrSubHead ++ dateSubHead,
    links = Seq(Link(
      id = "view-details", url = dummyUrl, linkText = messages(linkText), notification = notificationText
    ))
  )

  private def manageReportsEventsCard(erHtml:Html, subHeadingPstr: String)
                     (implicit messages: Messages): CardViewModel = {
    val aftLink = Seq(Link(
        id = "aft-view-link",
        url = dummyUrl,
        linkText = messages("messages__aft__view_details_link")
      ))

    val erLink = if (erHtml.equals(Html(""))) {
      Seq()
    } else {
      Seq(Link(
        id = "er-view-link",
        url = dummyUrl,
        linkText = messages("messages__er__view_details_link")
      ))
    }
    val psrLink = Seq(
      Link(
        id = "psr-view-details",
        url = dummyUrl,
        linkText = messages("messages__psr__view_details_link")
      ))



    CardViewModel(
      id = "manage_reports_returns",
      heading = Message("messages__manage_reports_and_returns_head"),
      links =  if(subHeadingPstr.isBlank) { aftLink ++ erLink } else { aftLink ++ erLink ++ psrLink }
    )
  }

  private def closedSchemeCard(linkText: String = "messages__psaSchemeDash__view_details_link")(implicit messages: Messages): CardViewModel = CardViewModel(
    id = "scheme_details",
    heading = Message("messages__psaSchemeDash__scheme_details_head"),
    subHeadings = pstrSubHead,
    links = Seq(Link(
      id = "view-details",
      url = dummyUrl,
      linkText = messages(linkText),
      notification = Some(Message("messages__psaSchemeDash__view_change_details_link_notification_scheme", "<strong>" + name + "</strong>"))
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


  def psaCardForInterimDashboard(inviteLink: Seq[Link] = inviteLink)
                                (implicit messages: Messages): CardViewModel =
    CardViewModel(
      id = "psa_psp_list",
      heading = Message("messages__psaSchemeDash__psa_psp_list_head"),
      subHeadings = Seq(CardSubHeading(
          subHeading = messages("messages__psaSchemeDash__addedOn_date", LocalDate.parse("2018-07-01").format(formatter)),
          subHeadingClasses = "card-sub-heading",
          subHeadingParams = Seq.empty[CardSubHeadingParam] )),
      links = inviteLink ++ Seq(
        Link(
          id = "view-psa-list",
          url = ViewAdministratorsController.onPageLoad(srn).url,
          linkText = Message("messages__psaSchemeDash__view_psa")
        ),
        Link(
          id = "authorise",
          url = WhatYouWillNeedController.onPageLoad(srn).url,
          linkText = Message("messages__pspAuthorise__link")
        ),
        Link(
          id = "view-practitioners",
          url = ViewPractitionersController.onPageLoad(srn).url,
          linkText = Message("messages__pspViewOrDeauthorise__link")
        )
      )
    )

  private def inviteLink = Seq(Link(
    id = "invite",
    url = InviteController.onPageLoad(srn).url,
    linkText = Message("messages__psaSchemeDash__invite_link")
  ))

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

  private def currentScheme(schemeStatus: SchemeStatus): Option[SchemeDetails] = Some(
    SchemeDetails(name = name,
      referenceNumber = srn,
      schemeStatus = schemeStatus.value,
      openDate = Some(date),
      windUpDate = Some(windUpDate),
      pstr = Some(pstr),
      relationship = None
    )
  )

}
