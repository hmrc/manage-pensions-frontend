/*
 * Copyright 2018 HM Revenue & Customs
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

package connectors

import javassist.tools.web.BadHttpRequest

import com.google.inject.{Inject, ImplementedBy}
import config.FrontendAppConfig
import uk.gov.hmrc.http.{NotFoundException, BadRequestException, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{Future, ExecutionContext}

@ImplementedBy(classOf[AssociationConnectorImpl])
trait AssociationConnector {

  def getSubscriptionDetails(psaId:String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]
}

class AssociationConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends AssociationConnector {

  override def getSubscriptionDetails(psaId:String) (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val psaIdHC = hc.withExtraHeaders("psaId"-> psaId)

    val url  = config.subscriptionDetailsUrl

    http.GET[HttpResponse](url)(implicitly, psaIdHC, implicitly) recoverWith {
      case ex : BadRequestException if(ex.message.contains("INVALID_PSAID")) => throw new PsaIdInvalidException
      case ex : BadRequestException if(ex.message.contains("INVALID_CORRELATIONID")) => throw new CorrelationIdInvalidException
      case ex : NotFoundException => throw new PsaIdNotFoundException
    }

  }
}