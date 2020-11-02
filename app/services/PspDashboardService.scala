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
import controllers.routes._
import javax.inject.Inject
import models.Link
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.{CardViewModel, Message}

import scala.concurrent.{ExecutionContext, Future}

class PspDashboardService @Inject()(appConfig: FrontendAppConfig,
                                    minimalConnector: MinimalConnector
                                   )(implicit ec: ExecutionContext) {

  def getPspName(pspId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    minimalConnector.getNameFromPspID(pspId).map(identity)

  def getTiles(pspId: String)(implicit messages: Messages): Seq[CardViewModel] =
    Seq(schemeCard, practitionerCard(pspId))

  private def practitionerCard(pspId: String)(implicit messages: Messages): CardViewModel =

    CardViewModel(
      id = "practitioner-card",
      heading = Message("messages__pspDashboard__details_heading"),
      subHeading = Some(Message("messages__pspDashboard__psp_id")),
      subHeadingParam = Some(pspId),
      links = Seq(
        Link("pspLink", appConfig.pspDetailsUrl, Message("messages__pspDashboard__psp_change")),
        //todo change pspDeregisterUrl once page to redirect to is implemented
        Link("deregister-link", appConfig.pspDeregisterUrl, Message("messages__pspDashboard__psp_deregister"))
      )
    )

  private def schemeCard(implicit messages: Messages): CardViewModel =
    CardViewModel(
      id = "scheme-card",
      heading = Message("messages__pspDashboard__scheme_heading"),
      links = Seq(Link(
        "search-schemes",
        ListSchemesController.onPageLoad().url,   //todo change link once 4159 is implemented
        Message("messages__pspDashboard__search_scheme")))
    )
}
