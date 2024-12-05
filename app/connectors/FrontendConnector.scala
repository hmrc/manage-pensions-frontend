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

package connectors

import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.Logger
import play.api.http.Status.OK
import play.api.mvc.Request
import play.twirl.api.Html
import services.HeaderCarrierFunctions
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.partials.HtmlPartial

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

class FrontendConnector @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig) {

  private val logger = Logger(classOf[FrontendConnector])

  def retrieveAftPartial[A](srn: String)
                           (implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.aftPartialHtmlUrl.format(srn))
  def retrieveFinInfoPartial[A](srn: String)
                           (implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.finInfoPartialHtmlUrl.format(srn),timeout = Duration(40,SECONDS))

  def retrieveEventReportingPartial[A](implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.eventReportingPartialHtmlUrl)

  def retrievePaymentsAndChargesPartial[A](srn: String)
                                          (implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.paymentsAndChargesPartialHtmlUrl.format(srn))

  def retrieveSchemeUrlsPartial[A](implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.schemeUrlsPartialHtmlUrl)

  def retrievePenaltiesUrlPartial[A](implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.penaltiesUrlPartialHtmlUrl)

  def retrieveMigrationUrlsPartial[A](implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.migrationUrlsPartialHtmlUrl)

  def retrievePspSchemeDashboardCards[A](srn: String, pspId: String, authorisingPsaId: String)
                                           (implicit request: Request[A], ec: ExecutionContext): Future[Html] = {
    val extraHeaders: Seq[(String, String)] = Seq(
      ("idNumber", srn),
      ("schemeIdType", "srn"),
      ("authorisingPsaId", authorisingPsaId),
      ("psaId", pspId)
    )
    retrievePartial(config.pspSchemeDashboardCardsUrl, extraHeaders)
  }

  private def retrievePartial[A](url: String,
                                 extraHeaders: Seq[(String, String)] = Seq.empty,
                                 timeout:Duration = Duration(20, SECONDS))
                                (implicit request: Request[A], ec: ExecutionContext): Future[Html] = {

    implicit val hc: HeaderCarrier = HeaderCarrierFunctions.headerCarrierForPartials(request)
      .toHeaderCarrier
      .withExtraHeaders(extraHeaders: _*)

    httpClientV2.get(url"${url}")(hc)
      .transform(_.withRequestTimeout(timeout))
      .execute[HttpResponse]
      .flatMap(handleResponse)
      .recover(recoverHtmlPartial)
  }

  private def handleResponse(response: HttpResponse)(implicit ec: ExecutionContext): Future[Html] = {
    response.status match {
      case OK => Future.successful(Html(response.body))
      case _ =>
        logger.warn("Failed to retrieve partial")
        Future.successful(Html(""))
    }
  }

  private def recoverHtmlPartial(implicit ec: ExecutionContext): PartialFunction[Throwable, Html] = {
    HtmlPartial.connectionExceptionsAsHtmlPartialFailure.andThen {
      case HtmlPartial.Success(_, content) => content
      case HtmlPartial.Failure(_, _) =>
        logger.warn("Failed to retrieve partial")
        Html("")
    }
  }

}
