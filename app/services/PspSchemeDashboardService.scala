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

import config.FrontendAppConfig
import connectors.admin.MinimalConnector
import controllers.psp.deauthorise.self.routes._
import models.{AuthorisedPractitioner, Link, MinimalPSAPSP}
import play.api.i18n.Messages
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
                srn: String,
                pstr: String,
                openDate: Option[String],
                loggedInPsp: AuthorisedPractitioner,
                clientReference: Option[String]
              )(implicit messages: Messages): Seq[PspSchemeDashboardCardViewModel] =
    Seq(
      schemeCard(srn, pstr, openDate),
      practitionerCard(loggedInPsp, clientReference)
    )

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
        id = "scheme-card-detail",
        url = appConfig.pspTaskListUrl.format(srn),
        linkText = Message("messages__pspSchemeDashboard__view_details_link")
      ))
    )
}
