/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{Link, MinimalPSAPSP}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PspDashboardService @Inject()(appConfig: FrontendAppConfig,
                                    minimalConnector: MinimalConnector
                                   )(implicit ec: ExecutionContext) {

  def getPspDetails(pspId: String)(implicit hc: HeaderCarrier): Future[MinimalPSAPSP] =
    minimalConnector.getMinimalPspDetails(pspId)

  def getTiles(pspId: String, details: MinimalPSAPSP)(implicit messages: Messages): Seq[CardViewModel] =
    Seq(schemeCard, practitionerCard(pspId, details))

  private def practitionerCard(pspId: String, details: MinimalPSAPSP)(implicit messages: Messages): CardViewModel =

    CardViewModel(
      id = "practitioner-card",
      heading = Message("messages__pspDashboard__details_heading"),
      subHeadings = Seq(
        CardSubHeading(
          subHeading = Message("messages__pspDashboard__psp_id"),
          subHeadingClasses = "heading-small card-sub-heading",
          subHeadingParams = Seq(
            CardSubHeadingParam(
              subHeadingParam = pspId,
              subHeadingParamClasses = "font-small bold")))),
      links = Seq(
        Link("pspLink", appConfig.pspDetailsUrl, Message("messages__pspDashboard__psp_change")),
        //todo change pspDeregisterUrl once page to redirect to is implemented
        Link("deregister-link", deregisterLink(details), Message("messages__pspDashboard__psp_deregister"))
      )
    )

  private def deregisterLink(details: MinimalPSAPSP): String =
    if (details.individualDetails.nonEmpty) appConfig.pspDeregisterIndividualUrl else appConfig.pspDeregisterCompanyUrl

  private def schemeCard(implicit messages: Messages): CardViewModel =
    CardViewModel(
      id = "scheme-card",
      heading = Message("messages__pspDashboard__scheme_heading"),
      links = Seq(Link(
        "search-schemes",
        controllers.psp.routes.ListSchemesController.onPageLoad.url,
        Message("messages__pspDashboard__search_scheme")))
    )
}
