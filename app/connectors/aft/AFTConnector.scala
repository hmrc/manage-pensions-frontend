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
import models.{AFTOverview, AFTVersion, Quarters}
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
  def getListOfVersions(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[AFTVersion]]]

  def getAftOverview(pstr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AFTOverview]]

  def aftStartDate: LocalDate
  def aftEndDate: LocalDate
}

@Singleton
class AFTConnectorImpl @Inject()(http: HttpClient, config: FrontendAppConfig) extends AFTConnector with HttpResponseHelper {
  override def getListOfVersions(pstr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[AFTVersion]]] = {
    val url = config.aftListOfVersions
    val schemeHc = hc.withExtraHeaders("pstr" -> pstr, "startDate" -> "2020-04-01")
    http.GET[HttpResponse](url)(implicitly, schemeHc, implicitly).map { response =>
      require(response.status == Status.OK)
      Option(response.json.as[Seq[AFTVersion]])
    } andThen {
      logExceptions
    } recoverWith {
      translateExceptions()
    }
  }

  private def logExceptions: PartialFunction[Try[Option[Seq[AFTVersion]]], Unit] = {
    case Failure(t: Throwable) => Logger.error("Unable to retrieve list of versions", t)
  }

  private def translateExceptions(): PartialFunction[Throwable, Future[Option[Seq[AFTVersion]]]] = {
    case _: Exception => Future.successful(None)
  }

  override def getAftOverview(pstr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[AFTOverview]] = {
    val url = config.aftOverviewUrl

    val schemeHc = hc.withExtraHeaders(
      "pstr" -> pstr,
      "startDate" -> aftStartDate.toString,
      "endDate" -> aftEndDate.toString)

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

  def aftEndDate: LocalDate = Quarters.getCurrentQuarter.endDate

  def aftStartDate: LocalDate =  {
    val earliestStartDate = LocalDate.parse(config.quarterStartDate)
    val calculatedStartYear = aftEndDate.minusYears(config.aftNoOfYearsDisplayed).getYear
    val calculatedStartDate = LocalDate.of(calculatedStartYear, 1, 1)

    if(calculatedStartDate.isAfter(earliestStartDate)) {
      calculatedStartDate
    } else {
      earliestStartDate
    }
  }

}
