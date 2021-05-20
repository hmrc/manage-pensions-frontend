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

package services

import base.SpecBase
import config.FrontendAppConfig
import controllers.invitations.psp.routes._
import controllers.invitations.routes._
import controllers.psa.routes._
import controllers.psp.routes._
import identifiers.invitations.PSTRId
import identifiers.{SchemeStatusId, SchemeNameId}
import models.FeatureToggle.{Enabled, Disabled}
import models.FeatureToggleName.PSPAuthorisation
import models.SchemeStatus.Rejected
import models.SchemeStatus.Open
import models._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper.formatter
import utils.UserAnswers
import viewmodels._

import java.time.LocalDate
import scala.concurrent.Future

class PsaSchemeDashboardServiceSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaFutures {

  import PsaSchemeDashboardServiceSpec._

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val mockAppConfig = mock[FrontendAppConfig]
  private val mockFeatureToggle = mock[FeatureToggleService]

  private def service: PsaSchemeDashboardService =
    new PsaSchemeDashboardService(mockAppConfig, mockFeatureToggle)

  override def beforeEach(): Unit = {
    when(mockAppConfig.viewSchemeDetailsUrl).thenReturn(dummyUrl)
    super.beforeEach()
  }

  "schemeCard" must {
    "return model fron aft-frontend is Scheme status is open and psa holds the lock" in {
      service.schemeCard(srn, currentScheme, Some(VarianceLock), userAnswers(Open.value)) mustBe
        schemeCard("messages__psaSchemeDash__view_change_details_link")
    }

    "return model with view-only link for scheme if psa does not hold lock" in {
      service.schemeCard(srn, currentScheme, Some(SchemeLock), userAnswers(Open.value)) mustBe schemeCard()
    }

    "return not display subheadings if scheme is not open" in {
      val ua = UserAnswers().set(SchemeStatusId)(Rejected.value).asOpt.get

      service.schemeCard(srn, currentScheme, Some(SchemeLock), ua) mustBe closedSchemeCard()
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

  "pspCard" must {
    "return model when psps are present nad toggle is on" in {
      when(mockFeatureToggle.get(any())(any(), any())).thenReturn(Future.successful(Enabled(PSPAuthorisation)))
      whenReady(service.pspCard(userAnswers(Open.value))) {
        _ mustBe List(pspCard())
      }
    }

    "return empty list when psps are present & toggle is off" in {
      when(mockFeatureToggle.get(any())(any(), any())).thenReturn(Future.successful(Disabled(PSPAuthorisation)))
      whenReady(service.pspCard(userAnswers(Open.value))) {
        _ mustBe Nil
      }
    }
  }

}

object PsaSchemeDashboardServiceSpec {

  private val srn = "srn"
  private val pstr = "pstr"
  private val schemeName = "Benefits Scheme"
  private val name = "test-name"
  private val date = "2020-01-01"
  private val dummyUrl = "dummy"
  private val minimalPsaName: Option[String] = Some("John Doe Doe")
  private val administrators: Option[Seq[AssociatedPsa]] =
    Some(
      Seq(
        AssociatedPsa("partnership name 2", canRemove = true),
        AssociatedPsa("Tony A Smith", canRemove = false)
      )
    )
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

  private def schemeCard(linkText: String = "messages__psaSchemeDash__view_details_link")(implicit messages: Messages): CardViewModel = CardViewModel(
    id = "scheme_details",
    heading = Message("messages__psaSchemeDash__scheme_details_head"),
    subHeadings = pstrSubHead ++ dateSubHead,
    links = Seq(Link("view-details", dummyUrl, messages(linkText)))
  )

  private def closedSchemeCard(linkText: String = "messages__psaSchemeDash__view_details_link")(implicit messages: Messages): CardViewModel = CardViewModel(
    id = "scheme_details",
    heading = Message("messages__psaSchemeDash__scheme_details_head"),
    subHeadings = pstrSubHead,
    links = Seq(Link("view-details", dummyUrl, messages(linkText)))
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
      Link("authorise", WhatYouWillNeedController.onPageLoad().url,
        Message("messages__pspAuthorise__link")),
      Link("view-practitioners", ViewPractitionersController.onPageLoad().url,
        linkText = Message("messages__pspViewOrDeauthorise__link")
      ))
  )

  private val currentScheme:Option[SchemeDetails] = Some(SchemeDetails(name, srn, "Open", Some(date), Some(pstr), None))

}

