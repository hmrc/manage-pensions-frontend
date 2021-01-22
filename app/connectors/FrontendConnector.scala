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

package connectors

import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.Logger
import play.api.mvc.Request
import play.twirl.api.Html
import services.HeaderCarrierFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.play.partials.HtmlPartial.connectionExceptionsAsHtmlPartialFailure

import scala.concurrent.{ExecutionContext, Future}

class FrontendConnector @Inject()(http: HttpClient, config: FrontendAppConfig) {

  private val logger = Logger(classOf[FrontendConnector])

  def retrieveAftPartial[A](srn: String)
                           (implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.aftPartialHtmlUrl.format(srn))

  def retrievePaymentsAndChargesPartial[A](srn: String)
                                          (implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.paymentsAndChargesPartialHtmlUrl.format(srn))

  def retrieveSchemeUrlsPartial[A](implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.schemeUrlsPartialHtmlUrl)

  def retrievePenaltiesUrlPartial[A](implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.penaltiesUrlPartialHtmlUrl)

  def retrievePspDashboardAftReturnsCard[A](srn: String, pspId: String, authorisingPsaId: String)
                                           (implicit request: Request[A], ec: ExecutionContext): Future[Html] = {
    val extraHeaders: Seq[(String, String)] = Seq(
      ("idNumber", srn),
      ("schemeIdType", "srn"),
      ("authorisingPsaId", authorisingPsaId),
      ("psaId", pspId)
    )
    retrievePartial(config.pspDashboardAftReturnsCardUrl, extraHeaders)
  }

  def retrievePspDashboardUpcomingAftChargesCard[A](srn: String)
                                                   (implicit request: Request[A], ec: ExecutionContext): Future[Html] = {
    val extraHeaders: Seq[(String, String)] = Seq(
      ("idNumber", srn)
    )
    retrievePartial(config.pspDashboardUpcomingAftChargesUrl, extraHeaders)
  }

  def retrievePspDashboardOverdueAftChargesCard[A](srn: String)
                                                  (implicit request: Request[A], ec: ExecutionContext): Future[Html] = {
    val extraHeaders: Seq[(String, String)] = Seq(
      ("idNumber", srn)
    )
    retrievePartial(config.pspDashboardOverdueAftChargesUrl, extraHeaders)
  }

  private def retrievePartial[A](url: String, extraHeaders: Seq[(String, String)] = Seq.empty)
                                (implicit request: Request[A], ec: ExecutionContext): Future[Html] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierFunctions
        .headerCarrierForPartials(request)
        .toHeaderCarrier
        .withExtraHeaders(extraHeaders: _*)

    http.GET[HtmlPartial](url) recover connectionExceptionsAsHtmlPartialFailure map {
      case HtmlPartial.Success(_, content) =>
        content
      case HtmlPartial.Failure(_, _) =>
        logger.warn("Failed to retrieve AFT partial")
        Html("")
    }
  }

}
