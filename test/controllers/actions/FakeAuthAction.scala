/*
 * Copyright 2019 HM Revenue & Customs
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

import models.{Individual, UserType}
import models.requests.AuthenticatedRequest
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.domain.PsaId

import scala.concurrent.Future

object FakeAuthAction {
  private val externalId: String = "id"
  private val defaultPsaId: String = "A0000000"

  def apply(): AuthAction = {
    new AuthAction {
      override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
        block(AuthenticatedRequest(request, externalId, PsaId(defaultPsaId), Individual))
    }
  }

  def createWithPsaId(psaId:String): AuthAction = {
    new AuthAction {
      override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
        block(AuthenticatedRequest(request, externalId, PsaId(psaId), Individual))
    }
  }

  def createUserType(userType:UserType): AuthAction = {
    new AuthAction {
      override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
        block(AuthenticatedRequest(request, externalId, PsaId(defaultPsaId), userType))
    }
  }
}

