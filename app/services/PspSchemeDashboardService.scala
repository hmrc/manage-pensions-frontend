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
import models.{Link, MinimalPSAPSP}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.{PspSchemeDashboardCardViewModel, Message}

import scala.concurrent.{ExecutionContext, Future}

class PspSchemeDashboardService @Inject()(appConfig: FrontendAppConfig,
                                          minimalConnector: MinimalConnector
                                         )(implicit ec: ExecutionContext) {

  def getPspDetails(pspId: String)
                   (implicit hc: HeaderCarrier): Future[MinimalPSAPSP] =
    minimalConnector.getMinimalPspDetails(pspId)

  def getTiles(pspId: String)
              (implicit messages: Messages): Seq[PspSchemeDashboardCardViewModel] =
    Seq(
//      schemeCard,
      practitionerCard(pspId)
    )

  private def practitionerCard(pspId: String)
                              (implicit messages: Messages): PspSchemeDashboardCardViewModel =

    PspSchemeDashboardCardViewModel(
      id = "your-practitioner-card",
      heading = Message("messages__pspSchemeDashboard__details_heading"),
      subHeadings = Seq(
        (Message("messages__pspSchemeDashboard__details_authBy"), "Dave"),
        (Message("messages__pspSchemeDashboard__details_authDate"), "14 Sep 2020"),
        (Message("messages__pspSchemeDashboard__details_clientRef"), "01234577")
      ),
      links = Seq(
        Link(
          id = "deauthorise-yourself",
          url = controllers.remove.pspSelfRemoval.routes.ConfirmRemovalController.onPageLoad().url,
          linkText = Message("De Authorise yourself")
        )
      )
    )

  private def schemeCard(implicit messages: Messages): PspSchemeDashboardCardViewModel =
    PspSchemeDashboardCardViewModel(
      id = "scheme-card",
      heading = Message("messages__pspDashboard__scheme_heading"),
      links = Seq(Link(
        "search-schemes",
        controllers.psp.routes.ListSchemesController.onPageLoad().url,
        Message("messages__pspDashboard__search_scheme")))
    )
}
