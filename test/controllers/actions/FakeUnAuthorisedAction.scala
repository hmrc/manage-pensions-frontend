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

package controllers.actions

import controllers.routes
import models.AuthEntity
import models.AuthEntity.PSA
import models.requests.AuthenticatedRequest
import play.api.mvc.Results._
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object FakeUnAuthorisedAction extends AuthAction {
  def apply(authEntity: AuthEntity = PSA): Auth = {
    new Auth {
      override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))

      val parser: BodyParser[AnyContent] = stubMessagesControllerComponents().parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    }
  }
}
