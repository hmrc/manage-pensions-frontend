/*
 * Copyright 2022 HM Revenue & Customs
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

package audit

import com.google.inject.{ImplementedBy, Inject}
import config.FrontendAppConfig
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {

  def sendEvent[T <: AuditEvent](event: T)
                                (implicit rh: RequestHeader, ec: ExecutionContext): Unit

  def sendExtendedEvent[T <: ExtendedAuditEvent](event: T)
                                                (implicit rh: RequestHeader, ec: ExecutionContext): Unit
}

class AuditServiceImpl @Inject()(
                                  config: FrontendAppConfig,
                                  connector: AuditConnector
                                ) extends AuditService {

  private val logger = Logger(classOf[AuditServiceImpl])

  private implicit def toHc(request: RequestHeader): AuditHeaderCarrier =
    auditHeaderCarrier(HeaderCarrierConverter.fromRequestAndSession(request, request.session))

  def sendEvent[T <: AuditEvent](event: T)
                                (implicit rh: RequestHeader, ec: ExecutionContext): Unit = {

    val details = rh.toAuditDetails() ++ event.details
    logger.debug(s"[AuditService][sendEvent] sending ${event.auditType}")
    val result: Future[AuditResult] = connector.sendEvent(
      DataEvent(
        auditSource = config.appName,
        auditType = event.auditType,
        tags = rh.toAuditTags(
          transactionName = event.auditType,
          path = rh.path
        ),
        detail = details
      )
    )

    onComplete(result, event.auditType)
  }

  def sendExtendedEvent[T <: ExtendedAuditEvent](event: T)
                                                (implicit rh: RequestHeader, ec: ExecutionContext): Unit = {

    logger.debug(s"[AuditService][sendEvent] sending ${event.auditType}")
    val result: Future[AuditResult] = connector.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = config.appName,
        auditType = event.auditType,
        tags = rh.toAuditTags(
          transactionName = event.auditType,
          path = rh.path
        ),
        detail = event.details
      )
    )

    onComplete(result, event.auditType)
  }

  private def onComplete(auditResult: Future[AuditResult], auditType: String)
                        (implicit ec: ExecutionContext): Unit =
    auditResult onComplete {
      case Success(_) =>
        logger.debug(s"[AuditService][sendEvent] successfully sent $auditType")
      case Failure(e) =>
        logger.error(s"[AuditService][sendEvent] failed to send event $auditType", e)
    }
}

