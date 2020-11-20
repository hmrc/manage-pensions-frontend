/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.admin.MinimalConnector
import javax.inject.Inject
import models.{AuthorisedPractitioner, Link, MinimalPSAPSP}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper
import viewmodels.{Message, PspSchemeDashboardCardViewModel}

import scala.concurrent.{ExecutionContext, Future}

class PspSchemeDashboardService @Inject()(
                                           appConfig: FrontendAppConfig,
                                           minimalConnector: MinimalConnector
                                         )(implicit ec: ExecutionContext) {

  def getPspDetails(pspId: String)
                   (implicit hc: HeaderCarrier): Future[MinimalPSAPSP] =
    minimalConnector.getMinimalPspDetails(pspId)

  def getPsaDetails(psaId: String)
                   (implicit hc: HeaderCarrier): Future[MinimalPSAPSP] =
    minimalConnector.getMinimalPsaDetails(psaId)


  def getTiles(
                srn: String,
                pstr: String,
                openDate: Option[String],
                loggedInPsp: AuthorisedPractitioner,
                clientReference: Option[String]
              )(implicit messages: Messages, hc: HeaderCarrier): Seq[PspSchemeDashboardCardViewModel] =
    Seq(
      aftReturnsCard,
      aftUpcomingChargesCard,
      aftOverdueChargesCard,
      schemeCard(srn, pstr, openDate),
      practitionerCard(loggedInPsp, clientReference)
    )

  private def practitionerCard(
                                loggedInPsp: AuthorisedPractitioner,
                                clientReference: Option[String]
                              )(implicit messages: Messages, hc: HeaderCarrier): PspSchemeDashboardCardViewModel = {

    val authedBy: String = loggedInPsp.authorisingPSA.name
    val relationshipStartDate: String = loggedInPsp.relationshipStartDate.format(DateHelper.formatter)

    PspSchemeDashboardCardViewModel(
      id = "practitioner-card",
      heading = Message("messages__pspSchemeDashboard__details_heading"),
      subHeadings = Seq(
        (Message("messages__pspSchemeDashboard__details__subHeading_authBy"), authedBy),
        (Message("messages__pspSchemeDashboard__details__subHeading_authDate"), relationshipStartDate)
      ),
      optionalSubHeadings = Seq(
        clientReference match {
          case Some(ref) =>
            Some(Message("messages__pspSchemeDashboard__details__subHeading_clientRef"), ref)
          case _ =>
            None
        }
      ),
      links = Seq(
        Link(
          id = "deauthorise-yourself",
          url = controllers.remove.pspSelfRemoval.routes.ConfirmRemovalController.onPageLoad().url,
          linkText = Message("De Authorise yourself")
        )
      )
    )
  }

  private def schemeCard(
                          srn: String,
                          pstr: String,
                          openDate: Option[String]
                        )(implicit messages: Messages): PspSchemeDashboardCardViewModel =
    PspSchemeDashboardCardViewModel(
      id = "scheme-card",
      heading = Message("messages__pspSchemeDashboard__scheme_heading"),
      subHeadings = Seq(
        (Message("messages__pspSchemeDashboard__scheme__subHeading_pstr"), pstr)
      ),
      optionalSubHeadings = Seq(
        openDate match {
          case Some(date) =>
            Some(Message("messages__pspSchemeDashboard__scheme__subHeading_regForTax"), date)
          case _ =>
            None
        }
      ),
      links = Seq(Link(
        id = "search-schemes",
        url = controllers.routes.PspSchemeDashboardController.onPageLoad(srn).url,
        linkText = Message("messages__pspSchemeDashboard__view_details_link")
      ))
    )

  def aftReturnsCard(implicit messages: Messages): PspSchemeDashboardCardViewModel =
    PspSchemeDashboardCardViewModel(
      id = "aft-returns",
      heading = Message("messages__pspSchemeDashboard__aftReturns_heading"),
      subHeadings = Seq(
        (Message("messages__pspSchemeDashboard__aftReturns__subHeading_aftDetails"), "In progress")
      ),
      links = Seq(
        Link(
          id = "view-aft-return",
          url = "#",
          linkText = Message("messages__pspSchemeDashboard__aftReturns__actions_viewLockReturn")
        ),
        Link(
          id = "start-aft-return",
          url = "#",
          linkText = Message("messages__pspSchemeDashboard__aftReturns__actions_startNewReturn")
        ),
        Link(
          id = "view-amend-past-aft-return",
          url = "#",
          linkText = Message("messages__pspSchemeDashboard__aftReturns__actions_viewOrAmend")
        )
      )
    )

  def aftUpcomingChargesCard(implicit messages: Messages): PspSchemeDashboardCardViewModel =
    PspSchemeDashboardCardViewModel(
      id = "aft-upcoming-charges",
      heading = Message("messages__pspSchemeDashboard__aftUpcomingCharges_heading"),
      subHeadings = Seq(
        (Message("messages__pspSchemeDashboard__aftUpcomingCharges__subHeading_dueDate"), "£23.50")
      ),
      subHeadingParam = "detail-large",
      links = Seq(
        Link(
          id = "view-payments-charges",
          url = "#",
          linkText = Message("messages__pspSchemeDashboard__aftUpcomingCharges__actions_viewPayments")
        ),
        Link(
          id = "view-past-payments-charges",
          url = "#",
          linkText = Message("messages__pspSchemeDashboard__aftUpcomingCharges__actions_viewPastPayments")
        )
      )
    )

  def aftOverdueChargesCard(implicit messages: Messages): PspSchemeDashboardCardViewModel =
    PspSchemeDashboardCardViewModel(
      id = "aft-overdue-charges",
      heading = Message("messages__pspSchemeDashboard__aftOverdueCharges_heading"),
      subHeadings = Seq(
        (Message("messages__pspSchemeDashboard__aftOverdueCharges__subHeading_total"), "£6,000.00"),
        (Message("messages__pspSchemeDashboard__aftOverdueCharges__subHeading_interest"), "£155.81"),
      ),
      subHeadingParam = "detail-large",
      links = Seq(
        Link(
          id = "view-overdue-payments-interest-charges",
          url = "#",
          linkText = Message("messages__pspSchemeDashboard__aftOverdueCharges__actions_view")
        )
      )
    )
}
