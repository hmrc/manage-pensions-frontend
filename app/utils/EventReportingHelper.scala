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

package utils

import connectors.UserAnswersCacheConnector
import identifiers.EventReportingId
import models.{EventReporting, ListOfSchemes}
import models.requests.AuthenticatedRequest
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}

object EventReportingHelper {

  final case class EventReportingData(data:EventReporting, sessionStorageKey:String)
  def eventReportingData(srn:String, listOfSchemes: ListOfSchemes, dataNoPstr: String => EventReporting)
                        (implicit request:AuthenticatedRequest[_]): Option[EventReportingData] = {
    val pstr = listOfSchemes.schemeDetails.flatMap (_.find (_.referenceNumber.contains (srn) ) ).flatMap (_.pstr)
    (request.session.get(SessionKeys.sessionId), pstr) match {
      case (Some(sessionId), Some(pstr)) => Some(
        EventReportingData(
          dataNoPstr(pstr),
          sessionId
        )
      )
      case _ => None
    }
  }
  def storeData(sessionCacheConnector: UserAnswersCacheConnector, data:EventReportingData)
               (implicit hc:HeaderCarrier, ec:ExecutionContext): Future[JsValue] = {
    sessionCacheConnector.upsert(data.sessionStorageKey, Json.toJson(Map(EventReportingId.toString -> data.data)))
  }
}
