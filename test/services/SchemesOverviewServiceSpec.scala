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
import connectors._
import connectors.admin.MinimalConnector
import models._
import models.requests.OptionalDataRequest
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.twirl.api.Html
import testhelpers.InvitationBuilder.{invitation1, invitationList}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import scala.concurrent.Future

class SchemesOverviewServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  import SchemesOverviewServiceSpec._

  private val minimalPsaConnector: MinimalConnector = mock[MinimalConnector]
  private val frontendConnector = mock[FrontendConnector]
  private val invitationsCacheConnector = mock[InvitationsCacheConnector]

  override def beforeEach(): Unit = {
    when(minimalPsaConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any()))
      .thenReturn(Future.successful(minimalPsaName))
    when(invitationsCacheConnector.getForInvitee(any())(any(), any()))
      .thenReturn(Future.successful(invitationList))
    when(frontendConnector.retrieveSchemeUrlsPartial(any(), any())).thenReturn(Future.successful(html))
    when(frontendConnector.retrievePenaltiesUrlPartial(any(), any())).thenReturn(Future.successful(html))
    super.beforeEach()
  }

  def service: SchemesOverviewService =
    new SchemesOverviewService(frontendAppConfig, minimalPsaConnector,
      invitationsCacheConnector, frontendConnector)

  "getTiles" must {

    "return tiles with relevant links" when {
      "when all possible links are displayed" in {

        whenReady(service.getTiles(psaId)) {
          _ mustBe tiles()
        }

      }

      "psa is not invited to any schemes" in {
        when(invitationsCacheConnector.getForInvitee(any())(any(), any()))
          .thenReturn(Future.successful(Nil))

        whenReady(service.getTiles(psaId)) {
          _ mustBe tiles(adminCard(invitation = noInvitationsLink))
        }
      }

      "psa is invited to administer only one scheme" in {
        when(invitationsCacheConnector.getForInvitee(any())(any(), any()))
          .thenReturn(Future.successful(List(invitation1)))

        whenReady(service.getTiles(psaId)) {
          _ mustBe tiles(adminCard(invitation = oneInvitationsLink))
        }
      }

    }
  }

}

object SchemesOverviewServiceSpec extends SpecBase with MockitoSugar  {

  implicit val request: OptionalDataRequest[AnyContent] =
    OptionalDataRequest(FakeRequest("", ""), "id", Some(UserAnswers()), Some(PsaId("A0000000")))
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val html: Html = Html("test-html")
  val psaName: String = "John Doe"
  val schemeName = "Test Scheme Name"
  val timestamp: Long = System.currentTimeMillis
  private val psaId = "A0000000"
  private val srn = "srn"
  private val formatter = DateTimeFormatter.ofPattern("dd MMMM YYYY")

  val deleteDate: String = LocalDate.now(ZoneOffset.UTC).plusDays(frontendAppConfig.daysDataSaved).format(formatter)

  def minimalPsaDetails(psaSuspended: Boolean): MinimalPSAPSP = MinimalPSAPSP("test@test.com", psaSuspended, Some("Org Name"), None,
    rlsFlag = false, deceasedFlag = false)

  val minimalPsaName: Option[String] = Some("John Doe Doe")
  val minimalPsaOrgName: Option[String] = Some("Org Name")
  val expectedPsaOrgName: Option[String] = Some("Org Name")
  val individualPsaDetailsWithNoMiddleName: Option[String] = Some("John Doe")
  val minimalPsaDetailsOrg: MinimalPSAPSP = MinimalPSAPSP("test@test.com", isPsaSuspended = false, Some("Org Name"), None,
    rlsFlag = false, deceasedFlag = false)
  val expectedName: String = "John Doe Doe"

  val schemeNameJsonOption: JsObject = Json.obj("schemeName" -> schemeName)
  val schemeSrnNumberOnlyData: Option[JsObject] = Some(Json.obj("submissionReferenceNumber" -> Json.obj("schemeReferenceNumber" -> srn)))

  private def adminCard(deregistration: Seq[Link] = deregisterLink,
                        invitation: Seq[Link] = invitationsLink) = CardViewModel(
    id = "administrator-card",
    heading = Message("messages__schemeOverview__psa_heading"),
    subHeadings = Seq(CardSubHeading(
      subHeading = Message("messages__schemeOverview__psa_id"),
      subHeadingClasses = "heading-small card-sub-heading",
      subHeadingParams = Seq(CardSubHeadingParam(
        subHeadingParam = psaId,
        subHeadingParamClasses = "font-small")))),
    links = Seq(
      Link("psaLink", frontendAppConfig.registeredPsaDetailsUrl, Message("messages__schemeOverview__psa_change"))
    ) ++ invitation ++ deregistration,
    html = Some(html))

  private def schemeCard = CardViewModel(
    id = "scheme-card",
    heading = Message("messages__schemeOverview__scheme_heading"),
    links = Seq(
      Link("view-schemes", controllers.routes.ListSchemesController.onPageLoad().url, Message("messages__schemeOverview__scheme_view"))
    ),
    html = Some(html)
  )

  private val deregisterLink = Seq(Link("deregister-link", frontendAppConfig.psaDeregisterUrl,
    Message("messages__schemeOverview__psa_deregister")))

  private val invitationsLink = Seq(Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
    Message("messages__schemeOverview__psa_view_more_invitations", 2)))

  private val noInvitationsLink = Seq(Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
    Message("messages__schemeOverview__psa_view_no_invitation")))

  private val oneInvitationsLink = Seq(Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
    Message("messages__schemeOverview__psa_view_one_invitation")))

  private def tiles(admin: CardViewModel = adminCard(), scheme: CardViewModel = schemeCard): Seq[CardViewModel] = Seq(admin, scheme)
}




