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

package connectors

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import models.SendEmailRequest
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

sealed trait EmailStatus

case object EmailSent extends EmailStatus
case object EmailNotSent extends EmailStatus

@ImplementedBy(classOf[EmailConnectorImpl])
trait EmailConnector {

  def sendEmail(email: SendEmailRequest
               )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailStatus]

}

class EmailConnectorImpl @Inject()(appConfig: FrontendAppConfig, httpClientV2: HttpClientV2)
  extends EmailConnector {

  private val logger = Logger(classOf[EmailConnectorImpl])

  override def sendEmail(email: SendEmailRequest
                        )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailStatus] = {
    logger.debug("Sending email:" + email)
    val emailUrl = url"${appConfig.emailUrl}"

    httpClientV2.post(emailUrl)
      .withBody(Json.toJson(email))
      .execute[HttpResponse].map { response =>
        response.status match {
          case ACCEPTED =>
            logger.debug("Email sent successfully")
            EmailSent
          case status =>
            logger.warn(s"Email API returned non-expected response:$status")
            EmailNotSent
        }
      } recoverWith {
        case t: Throwable =>
          logger.warn("Email API call failed", t)
          Future.successful(EmailNotSent)
      }
  }

}
