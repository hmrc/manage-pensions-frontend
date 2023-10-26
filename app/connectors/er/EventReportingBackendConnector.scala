/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors.er

import com.google.inject.Singleton
import config.FrontendAppConfig
import models.RequiredEventReportingJourneyData
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventReportingBackendConnector @Inject() (config: FrontendAppConfig, http: HttpClient)(implicit ec: ExecutionContext){
  def createNewJourney(requiredJourneyData: RequiredEventReportingJourneyData)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val json = Json.toJson(requiredJourneyData)
    http.POST[JsValue, HttpResponse](config.createNewEventReportingJourneyUrl, json)(implicitly, implicitly, hc, implicitly)
  }
}
