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
import connectors.admin.MinimalConnector
import controllers.psp.deauthorise.self.routes._
import models.{AuthorisedPractitioner, Link, MinimalPSAPSP}
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper
import viewmodels.{Message, PspSchemeDashboardCardViewModel}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PspSchemeDashboardService @Inject()(
                                           appConfig: FrontendAppConfig,
                                           minimalConnector: MinimalConnector
                                         )(implicit ec: ExecutionContext) {

  def getMinimalPspDetails(pspId: String)
                          (implicit hc: HeaderCarrier): Future[MinimalPSAPSP] =
    minimalConnector.getMinimalPspDetails(pspId)

  def getTiles(
                interimDashboard: Boolean,
                erHtml: Html,
                srn: String,
                pstr: String,
                openDate: Option[String],
                loggedInPsp: AuthorisedPractitioner,
                clientReference: Option[String]
              )(implicit messages: Messages): Seq[PspSchemeDashboardCardViewModel] = {
    if (interimDashboard) {
      Seq(manageReportsEventsCard(srn, erHtml), practitionerCard(loggedInPsp, clientReference))
    } else {
      Seq(schemeCard(srn, pstr, openDate), practitionerCard(loggedInPsp, clientReference))
    }
  }

  private def practitionerCard(
                                loggedInPsp: AuthorisedPractitioner,
                                clientReference: Option[String]
                              )(implicit messages: Messages): PspSchemeDashboardCardViewModel = {

    val authedBy: String = loggedInPsp.authorisingPSA.name
    val relationshipStartDate: String = loggedInPsp.relationshipStartDate.format(DateHelper.formatter)

    PspSchemeDashboardCardViewModel(
      id = "practitioner-card",
      heading = Message("messages__pspSchemeDashboard__details_heading"),
      subHeadings = Seq(
        (Message("messages__pspSchemeDashboard__details__subHeading_authBy"), authedBy),
        (Message("messages__pspSchemeDashboard__details__subHeading_authDate"), relationshipStartDate)
      ),
      optionalSubHeading = clientReference map {
        ref =>
          (Message("messages__pspSchemeDashboard__details__subHeading_clientRef"), ref)
      },
      links = Seq(
        Link(
          id = "deauthorise-yourself",
          url = ConfirmDeauthController.onPageLoad().url,
          linkText = Message("messages__pspSchemeDashboard__details__deAuth_Link"),
          hiddenText = Some(Message("messages__pspSchemeDashboard__details__deAuth_Link_screenReaderAlternative"))
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
      optionalSubHeading = openDate map {
        date =>
          (Message("messages__pspSchemeDashboard__scheme__subHeading_regForTax"), date)
      },
      links = Seq(Link(
        id = "view-details",
        url = appConfig.pspTaskListUrl.format(srn),
        linkText = Message("messages__pspSchemeDashboard__view_details_link")
      ))
    )

  private def manageReportsEventsCard(srn: String, erHtml:Html)
                               (implicit messages: Messages): PspSchemeDashboardCardViewModel =
    {
      val aftLink = Seq(Link(
          id = "aft-view-link",
          url = appConfig.aftOverviewHtmlUrl.format(srn),
          linkText = messages("messages__aft__view_details_link")
        ))

      val erLink = if (erHtml.equals(Html(""))) {
        Seq()
      } else {
        Seq(Link(
          id = "er-view-link",
          url = appConfig.eventReportingOverviewHtmlUrl.format(srn),
          linkText = messages("messages__er__view_details_link")
        ))
      }

      val psrLink = Seq(
        Link(
          id = "psr-view-details",
          url = appConfig.psrPartialHtmlUrl.format(srn),
          linkText = messages("messages__psr__view_details_link")
        ))

      PspSchemeDashboardCardViewModel(
        id = "manage_reports_returns",
        heading = Message("messages__manage_reports_and_returns_head"),
        links = aftLink ++ erLink ++ psrLink
      )
    }
}
