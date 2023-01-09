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

package controllers.actions


import com.google.inject.ImplementedBy
import com.google.inject.Inject
import connectors.UserAnswersCacheConnector
import models.requests.AuthenticatedRequest
import models.requests.OptionalDataRequest
import play.api.mvc.ActionTransformer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class DataRetrievalActionImpl @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
                                       (implicit val executionContext: ExecutionContext) extends DataRetrievalAction {

  override protected def transform[A](request: AuthenticatedRequest[A]): Future[OptionalDataRequest[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    dataCacheConnector.fetch(request.externalId).map {
      case None =>
        OptionalDataRequest(request.request, request.externalId, None, request.psaId, request.pspId)
      case Some(data) =>
        OptionalDataRequest(request.request, request.externalId, Some(UserAnswers(data)), request.psaId, request.pspId)
    }
  }
}

@ImplementedBy(classOf[DataRetrievalActionImpl])
trait DataRetrievalAction extends ActionTransformer[AuthenticatedRequest, OptionalDataRequest]
