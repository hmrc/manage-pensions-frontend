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

package connectors.aft

import java.time.LocalDate

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.FrontendAppConfig
import models.{AFTOverview, Quarters}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@ImplementedBy(classOf[AFTConnectorImpl])
trait AFTConnector {
  def getListOfVersions(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[Int]]]

  def getAftOverview(pstr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AFTOverview]]
}

@Singleton
class AFTConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends AFTConnector with HttpResponseHelper {
  override def getListOfVersions(pstr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[Int]]] = {
    val url = config.aftListOfVersions
    val schemeHc = hc.withExtraHeaders("pstr" -> pstr, "startDate" -> "2020-04-01")
    http.GET[HttpResponse](url)(implicitly, schemeHc, implicitly).map { response =>
      require(response.status == Status.OK)
      Option(response.json.as[Seq[Int]])
    } andThen {
      logExceptions
    } recoverWith {
      translateExceptions()
    }
  }

  override def getAftOverview(pstr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AFTOverview]] = {
    val url = config.aftOverviewUrl

    val numberOfYears = 6
    val endDate = Quarters.getCurrentQuarter.endDate
    val startYear = endDate.minusYears(numberOfYears).getYear
    val startDate = LocalDate.of(startYear, 1, 1)

    val schemeHc = hc.withExtraHeaders("pstr" -> pstr, "startDate" -> startDate.toString, "endDate" -> endDate.toString)

    http.GET[HttpResponse](url)(implicitly, schemeHc, implicitly).map { response =>
      response.status match {
        case OK =>
          val json = Json.parse(response.body)
          json.validate[Seq[AFTOverview]] match {
            case JsSuccess(value, _) => value
            case JsError(errors) => throw JsResultException(errors)
          }
        case _ => handleErrorResponse("GET", url)(response)
      }
    } andThen {
      case Failure(t: Throwable) => Logger.warn("Unable to get aft overview", t)
    }
  }

  private def logExceptions: PartialFunction[Try[Option[Seq[Int]]], Unit] = {
    case Failure(t: Throwable) => Logger.error("Unable to retrieve list of versions", t)
  }

  private def translateExceptions(): PartialFunction[Throwable, Future[Option[Seq[Int]]]] = {
    case ex: Exception => Future.successful(None)
  }
}
