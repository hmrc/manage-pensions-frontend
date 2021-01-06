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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext
import scala.concurrent.Future


class MongoDiagnosticsConnector @Inject()(http: HttpClient, config: FrontendAppConfig) {

  def fetchDiagnostics()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {

    for {
      admin <- http.GET[HttpResponse](s"${config.pensionAdminUrl}/test-only/mongo-diagnostics")
      scheme <- http.GET[HttpResponse](s"${config.pensionsSchemeUrl}/test-only/mongo-diagnostics")
    } yield {
      admin.body + "\n" + scheme.body
    }

  }

}
