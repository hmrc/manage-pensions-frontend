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

package models.requests

import controllers.actions.IdNotFound
import models.{AuthEntity, UserType}
import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.domain.{PsaId, PspId}

trait IdentifiedRequest {
  def externalId: String
}

case class AuthenticatedRequest[A](request: Request[A],
                                   externalId: String,
                                   psaId: Option[PsaId],
                                   pspId: Option[PspId] = None,
                                   userType: UserType,
                                   authEntity: AuthEntity)
  extends WrappedRequest[A](request) with IdentifiedRequest {
  def psaIdOrException: PsaId = psaId.getOrElse(throw new IdNotFound)

  def pspIdOrException: PspId = pspId.getOrElse(throw new IdNotFound)
}
