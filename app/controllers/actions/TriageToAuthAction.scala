/*
 * Copyright 2025 HM Revenue & Customs
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

import com.google.inject.Inject
import models.requests.{AuthenticatedRequest, TriageRequest}
import models.{AuthEntity, OtherUser}
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

class TriageToAuthAction @Inject()(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
  extends ActionTransformer[TriageRequest, AuthenticatedRequest] {

  override protected def transform[A](request: TriageRequest[A]): Future[AuthenticatedRequest[A]] = {
    val externalId = request.externalId
    Future.successful(
      AuthenticatedRequest(
        request = request.request,
        externalId = externalId,
        psaId = None,
        pspId = None,
        userType = OtherUser,
        authEntity = AuthEntity.Unauthenticated
      )
    )
  }
}