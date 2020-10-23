/*
 * Copyright 2020 HM Revenue & Customs
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

import models.AuthEntity
import models.AuthEntity.PSA
import models.AuthEntity.PSP
import models.requests.AuthenticatedRequest
import models.Individual
import models.UserType
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object FakeAuthAction extends AuthAction {
  override def apply(authEntity: AuthEntity = PSA): Auth = new FakeAuth(authEntity)
  def createWithPsaId(psaId:String): AuthAction = new AuthAction {
    override def apply(authEntity: AuthEntity = PSA): Auth = new FakeAuth(authEntity = PSA, psaId = Some(PsaId(psaId)), pspId = None)
  }
  def createWithUserType(userType:UserType): AuthAction = new AuthAction {
    override def apply(authEntity: AuthEntity = PSA): Auth = new FakeAuth(authEntity = PSA, pspId = None)
  }
  def createWithPspId(pspId:String): AuthAction = new AuthAction {
    override def apply(authEntity: AuthEntity = PSA): Auth = new FakeAuth(authEntity = PSP, pspId = Some(PspId(pspId)), psaId = None)
  }
  val externalId: String = "id"
}

class FakeAuth(authEntity: AuthEntity,
  psaId:Option[PsaId] = Some(PsaId("A0000000")), pspId:Option[PspId] = Some(PspId("00000000"))) extends Auth {
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    block(AuthenticatedRequest(request, "id", psaId, pspId, Individual, authEntity))

  val parser: BodyParser[AnyContent] = stubMessagesControllerComponents().parsers.defaultBodyParser

  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}


