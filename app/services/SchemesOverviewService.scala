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

import config.FrontendAppConfig
import connectors._
import connectors.admin.MinimalConnector
import controllers.psa.routes._
import models.requests.OptionalDataRequest
import models.{Link, MinimalPSAPSP}
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemesOverviewService @Inject()(
                                        appConfig: FrontendAppConfig,
                                        minimalPsaConnector: MinimalConnector,
                                        invitationsCacheConnector: InvitationsCacheConnector,
                                        frontendConnector: FrontendConnector
                                      )(implicit ec: ExecutionContext) {

  def getTiles(psaId: String)(implicit request: OptionalDataRequest[AnyContent], hc: HeaderCarrier, messages: Messages): Future[Seq[CardViewModel]] =
    for {
      invitationLink <- invitationsLink
      schemeHtml <- frontendConnector.retrieveSchemeUrlsPartial
    } yield {
      Seq(
        schemeCard(schemeHtml),
        adminCard(invitationLink, psaId)
      )
    }

  def retrievePenaltiesUrlPartial[A](implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    frontendConnector.retrievePenaltiesUrlPartial

  def retrieveMigrationTile[A](implicit request: Request[A], ec: ExecutionContext): Future[Option[Html]] =
    frontendConnector.retrieveMigrationUrlsPartial.map(Some(_))

  def getPsaName(psaId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    minimalPsaConnector.getPsaNameFromPsaID(psaId).map(identity)

  def getPsaMinimalDetails(psaId: String)(implicit hc: HeaderCarrier): Future[MinimalPSAPSP] =
    minimalPsaConnector.getMinimalPsaDetails(psaId)

  //TILES HELPER METHODS
  private def adminCard(invitationLink: Seq[Link], psaId: String)
                       (implicit messages: Messages): CardViewModel =

    CardViewModel(
      id = "administrator-card",
      heading = Message("messages__schemeOverview__psa_heading"),
      subHeadings = Seq(CardSubHeading(
        subHeading = Message("messages__schemeOverview__psa_id"),
        subHeadingClasses = "card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = psaId,
          subHeadingParamClasses = "font-small bold")))),
      links = Seq(
        Link("psaLink", appConfig.registeredPsaDetailsUrl, Message("messages__schemeOverview__psa_change"))
      ) ++ invitationLink ++ deregisterLink,
      html = None
    )

  private def schemeCard(html: Html)(implicit messages: Messages): CardViewModel =
    CardViewModel(
      id = "scheme-card",
      heading = Message("messages__schemeOverview__scheme_heading"),
      links = Seq(
        Link("view-schemes", ListSchemesController.onPageLoad.url, Message("messages__schemeOverview__scheme_view"))
      ),
      html = Some(html)
    )

  private def invitationsLink(implicit request: OptionalDataRequest[AnyContent], hc: HeaderCarrier): Future[Seq[Link]] =
    invitationsCacheConnector.getForInvitee(request.psaIdOrException).map { invitationsList =>
      val linkText = invitationsList match {
        case Nil => Message("messages__schemeOverview__psa_view_no_invitation")
        case invitations if invitations.size == 1 => Message("messages__schemeOverview__psa_view_one_invitation")
        case invitations => Message("messages__schemeOverview__psa_view_more_invitations", invitations.size)
      }
      Seq(Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url, linkText))
    }

  private val deregisterLink: Seq[Link] =
    Seq(Link("deregister-link", appConfig.psaDeregisterUrl,
      Message("messages__schemeOverview__psa_deregister")))


}
