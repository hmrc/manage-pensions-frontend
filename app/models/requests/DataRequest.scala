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
import play.api.mvc.Request
import play.api.mvc.WrappedRequest
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.domain.PspId
import utils.UserAnswers

import scala.language.implicitConversions

class OptionalDataRequest[A](override val request: Request[A],
                                  override val externalId: String,
                                  val userAnswers: Option[UserAnswers],
                                  override val psaId: Option[PsaId],
                                  override val pspId: Option[PspId] = None,
                                  override val userType: UserType,
                                  override val authEntity: AuthEntity)
  extends AuthenticatedRequest[A](request, externalId, psaId, pspId, userType, authEntity) with IdentifiedRequest {
  override def psaIdOrException:PsaId = psaId.getOrElse(throw IdNotFound())
  override def pspIdOrException:PspId = pspId.getOrElse(throw IdNotFound("PspIdNotFound"))
}

object OptionalDataRequest {
  def apply[A](request: Request[A],
  externalId: String,
  userAnswers: Option[UserAnswers],
  psaId: Option[PsaId],
  pspId: Option[PspId] = None,
  userType: UserType,
  authEntity: AuthEntity) = new OptionalDataRequest[A](request, externalId, userAnswers, psaId, pspId, userType, authEntity)
}

case class DataRequest[A](request: Request[A],
                          externalId: String,
                          userAnswers: UserAnswers,
                          psaId: Option[PsaId],
                          pspId: Option[PspId] = None,
                          userType: UserType,
                          authEntity: AuthEntity)
  extends WrappedRequest[A](request) with IdentifiedRequest {
  def psaIdOrException:PsaId = psaId.getOrElse(throw IdNotFound())
  def pspIdOrException:PspId = pspId.getOrElse(throw IdNotFound("PspIdNotFound"))
}

object DataRequest {
  implicit def toOptional[A](r: DataRequest[A]):OptionalDataRequest[A] = {
    OptionalDataRequest(r.request, r.externalId, Some(r.userAnswers), r.psaId, r.pspId, r.userType, r.authEntity)
  }
}