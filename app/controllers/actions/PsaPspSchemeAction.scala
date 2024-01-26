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
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionFunction, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsaPspSchemeActionImpl (srn:SchemeReferenceNumber, schemeDetailsConnector: SchemeDetailsConnector, errorHandler: ErrorHandler)
                          (implicit val executionContext: ExecutionContext)
  extends ActionFunction[OptionalDataRequest, OptionalDataRequest] with FrontendHeaderCarrierProvider {

  private def notFoundTemplate(implicit request: OptionalDataRequest[_]) = NotFound(errorHandler.notFoundTemplate)
  override def invokeBlock[A](request: OptionalDataRequest[A], block: OptionalDataRequest[A] => Future[Result]): Future[Result] = {
    request.psaId -> request.pspId match {
      case (Some(_), None) => new PsaSchemeAction(schemeDetailsConnector, errorHandler).apply(Some(srn)).invokeBlock(request, block)
      case (None, Some(_)) => new PspSchemeAction(schemeDetailsConnector, errorHandler).apply(srn).invokeBlock(request, block)
      case _ => Future.successful(notFoundTemplate(request))
    }
  }
}


class PsaPspSchemeAction @Inject() (schemeDetailsConnector: SchemeDetailsConnector, errorHandler: ErrorHandler)(implicit ec: ExecutionContext){
  def apply(srn: SchemeReferenceNumber) = new PsaPspSchemeActionImpl(srn, schemeDetailsConnector, errorHandler)

}
