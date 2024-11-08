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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class MongoDiagnosticsConnector @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig) {

  def fetchDiagnostics()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {

    val pensionAdminUrl = url"${config.pensionAdminUrl}/test-only/mongo-diagnostics"
    val pensionsSchemeUrl = url"${config.pensionsSchemeUrl}/test-only/mongo-diagnostics"

    for {
      admin  <- httpClientV2.get(pensionAdminUrl).execute[HttpResponse]
      scheme <- httpClientV2.get(pensionsSchemeUrl).execute[HttpResponse]
    } yield {
      admin.body + "\n" + scheme.body
    }

  }

}
