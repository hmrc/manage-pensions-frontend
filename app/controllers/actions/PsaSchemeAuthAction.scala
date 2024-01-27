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
import identifiers.SchemeSrnId
import models.SchemeReferenceNumber
import models.psa.PsaDetails
import models.requests.OptionalDataRequest
import play.api.Logging
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionFunction, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

private class PsaSchemeActionImpl (srnOpt:Option[SchemeReferenceNumber], schemeDetailsConnector: SchemeDetailsConnector, errorHandler: ErrorHandler)
                          (implicit val executionContext: ExecutionContext)
  extends ActionFunction[OptionalDataRequest, OptionalDataRequest] with FrontendHeaderCarrierProvider with Logging {



  private def notFoundTemplate(implicit request: OptionalDataRequest[_]) = NotFound(errorHandler.notFoundTemplate)

  override def invokeBlock[A](request: OptionalDataRequest[A], block: OptionalDataRequest[A] => Future[Result]): Future[Result] = {

    val retrievedSrn = {
      if(srnOpt.isDefined) {
        srnOpt
      } else {
        request.userAnswers.flatMap { ua =>
          ua.get(SchemeSrnId).map { SchemeReferenceNumber(_) }
        }
      }
    }

    val psaIdOpt = request.psaId

    (retrievedSrn, psaIdOpt) match {
      case (Some(srn), Some(psaId)) =>
        getUserAnswers(srn, psaId.id)(request).flatMap { userAnswers =>
          val admins = (userAnswers.json \ "psaDetails").as[Seq[PsaDetails]].map(_.id)
          if (admins.contains(psaId.id)) {
            block(request)
          } else {
            Future.successful(notFoundTemplate(request))
          }
        } recover {
          case _ =>
            notFoundTemplate(request)
        }
      case _ => Future.successful(notFoundTemplate(request))
    }

  }
  private def getUserAnswers(srn: SchemeReferenceNumber, psaId: String)
                            (implicit request: OptionalDataRequest[_]): Future[UserAnswers] = {
    request.userAnswers match {
      case Some(ua) if (ua.json \ "psaDetails").asOpt[Seq[PsaDetails]].nonEmpty => Future.successful(ua)
      case _ => schemeDetailsConnector.getSchemeDetails(
        psaId = psaId,
        idNumber = srn,
        schemeIdType = "srn"
      )
    }
  }
}


class PsaSchemeAuthAction @Inject()(schemeDetailsConnector: SchemeDetailsConnector, errorHandler: ErrorHandler)(implicit ec: ExecutionContext){
  def apply(srn: Option[SchemeReferenceNumber]): ActionFunction[OptionalDataRequest, OptionalDataRequest] =
    new PsaSchemeActionImpl(srn, schemeDetailsConnector, errorHandler)
}
