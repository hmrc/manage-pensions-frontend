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

package connectors

import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.Logger
import play.api.http.Status
import services.HeaderCarrierFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import viewmodels.AFTViewModel
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure


import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class FrontendConnector @Inject()(http: HttpClient, config: FrontendAppConfig) {

  def aftUrl(srn: String): String = config.aftPartialHtmlUrl.format(srn)

  def retrieveAftViewModel[A](srn: String)(implicit request: Request[A], ec: ExecutionContext): Future[Seq[AFTViewModel]] = {
    val url = config.aftPartialHtmlUrl.format(srn)
    implicit val hc: HeaderCarrier = HeaderCarrierFunctions.headerCarrierForPartials(request).toHeaderCarrier
    http.GET(url).map { response =>
      require(response.status == Status.OK)
     response.json.as[Seq[AFTViewModel]]
    } andThen {
      case Failure(t: Throwable) => Logger.error("Unable to retrieve aft partial", t)
    } recoverWith {
      case _: Exception => Future.successful(Nil)
    }
  }


}
