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

package connectors.scheme

import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import javax.inject.Inject
import javax.inject.Singleton
import models.Lock
import models.SchemeVariance
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsError
import play.api.libs.json.JsResultException
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Try

@ImplementedBy(classOf[PensionSchemeVarianceLockConnectorImpl])
trait PensionSchemeVarianceLockConnector {

  def isLockByPsaIdOrSchemeId(psaId: String, srn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Lock]]

  def getLock(psaId: String, srn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeVariance]]
  def getLockByPsa(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeVariance]]
  def getLockByScheme(srn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeVariance]]

  def releaseLock(psaId: String, srn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]

}

@Singleton
class PensionSchemeVarianceLockConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends PensionSchemeVarianceLockConnector {

  override def isLockByPsaIdOrSchemeId(psaId: String, srn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Lock]] = {

    implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
    }

    implicit val headerCarrier: HeaderCarrier = hc.withExtraHeaders("psaId" -> psaId, "srn" -> srn)

    val url = s"${config.updateSchemeDetailsUrl}/isLockByPsaOrScheme"

    http.GET[HttpResponse](url)(implicitly, headerCarrier, implicitly).map { response =>

      response.status match {
        case NOT_FOUND =>
          None
        case OK =>
          Json.parse(response.body).validate[Lock] match {
            case JsSuccess(value, _) => Some(value)
            case JsError(errors) => throw JsResultException(errors)
          }
        case _ =>
          throw new HttpException(response.body, response.status)
      }
    } andThen logExceptions("Unable to find the lock")
  }

  private def logExceptions[I](msg : String): PartialFunction[Try[I], Unit] = {
    case Failure(t: Throwable) => Logger.error(msg, t)
  }

  override def getLock(psaId: String, srn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeVariance]] = {

    implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
    }

    implicit val headerCarrier: HeaderCarrier = hc.withExtraHeaders("psaId" -> psaId, "srn" -> srn)

    val url = s"${config.updateSchemeDetailsUrl}/get-lock"

    http.GET[HttpResponse](url)(implicitly, headerCarrier, implicitly).map { response =>

      response.status match {
        case NOT_FOUND =>
          None
        case OK =>
          Json.parse(response.body).validate[SchemeVariance] match {
            case JsSuccess(value, _) => Some(value)
            case JsError(errors) => throw JsResultException(errors)
          }
        case _ =>
          throw new HttpException(response.body, response.status)
      }
    } andThen logExceptions("Unable to find the lock")
  }

  override def getLockByPsa(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeVariance]] = {

    implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
    }

    implicit val headerCarrier: HeaderCarrier = hc.withExtraHeaders("psaId" -> psaId)

    val url = s"${config.updateSchemeDetailsUrl}/get-lock-by-psa"

    http.GET[HttpResponse](url)(implicitly, headerCarrier, implicitly).map { response =>

      response.status match {
        case NOT_FOUND =>
          None
        case OK =>
          Json.parse(response.body).validate[SchemeVariance] match {
            case JsSuccess(value, _) => Some(value)
            case JsError(errors) => throw JsResultException(errors)
          }
        case _ =>
          throw new HttpException(response.body, response.status)
      }
    } andThen logExceptions("Unable to find the lock")
  }

  override def getLockByScheme(srn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeVariance]] = {

    implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
    }

    implicit val headerCarrier: HeaderCarrier = hc.withExtraHeaders("srn" -> srn)

    val url = s"${config.updateSchemeDetailsUrl}/get-lock-by-scheme"

    http.GET[HttpResponse](url)(implicitly, headerCarrier, implicitly).map { response =>

      response.status match {
        case NOT_FOUND =>
          None
        case OK =>
          Json.parse(response.body).validate[SchemeVariance] match {
            case JsSuccess(value, _) => Some(value)
            case JsError(errors) => throw JsResultException(errors)
          }
        case _ =>
          throw new HttpException(response.body, response.status)
      }
    } andThen logExceptions("Unable to find the lock")
  }

  override def releaseLock(psaId: String, srn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
    }

    implicit val headerCarrier: HeaderCarrier = hc.withExtraHeaders("psaId" -> psaId, "srn" -> srn)

    val url = s"${config.updateSchemeDetailsUrl}/release-lock"

    http.DELETE[HttpResponse](url)(implicitly, headerCarrier, implicitly).map { response =>

      response.status match {
        case OK =>{}
        case _ =>
          throw new HttpException(response.body, response.status)
      }
    } andThen logExceptions("Unable to release the lock")
  }

}
