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

package controllers.actions

import connectors.scheme.SchemeDetailsConnector
import handlers.ErrorHandler
import models.SchemeReferenceNumber
import models.requests.OptionalDataRequest
import play.api.mvc.{ActionFunction, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


private class FakePspSchemeActionImpl()
                                  (implicit val executionContext: ExecutionContext)
  extends ActionFunction[OptionalDataRequest, OptionalDataRequest] {


  override def invokeBlock[A](request: OptionalDataRequest[A], block: OptionalDataRequest[A] => Future[Result]): Future[Result] = {
    block(request)
  }
}
class FakePspSchemeAuthAction @Inject()(schemeDetailsConnector: SchemeDetailsConnector, errorHandler: ErrorHandler)
                                       (implicit ec: ExecutionContext) extends PspSchemeAuthAction(schemeDetailsConnector, errorHandler) {
  override def apply(srn: SchemeReferenceNumber): ActionFunction[OptionalDataRequest, OptionalDataRequest] = new FakePspSchemeActionImpl()
}
