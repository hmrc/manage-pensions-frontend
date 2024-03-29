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

package connectors.aft

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.CacheConnector
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import play.api.mvc.Results.Ok

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AftCacheConnector @Inject()(
                                   config: FrontendAppConfig,
                                   http: WSClient
                                 ) {

  private def url = s"${config.aftUrl}/pension-scheme-accounting-for-tax/journey-cache/aft/lock"

  def removeLock(
                  implicit ec: ExecutionContext,
                  hc: HeaderCarrier
                ): Future[Result] =
    http
      .url(url)
      .withHttpHeaders(CacheConnector.headers(hc): _*)
      .delete()
      .map(_ => Ok)
}
