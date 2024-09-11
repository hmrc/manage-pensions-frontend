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

import com.google.inject.Inject
import config.FrontendAppConfig
import models.{EROverview, SchemeReferenceNumber}
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http._
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}

class PensionSchemeReturnConnector @Inject()(
                                         config: FrontendAppConfig,
                                         http: HttpClient
                                       )(implicit ec: ExecutionContext) extends HttpResponseHelper {

  def getOverview(srn: SchemeReferenceNumber, pstr: String, startDate: String, endDate: String)
                 (implicit headerCarrier: HeaderCarrier): Future[Seq[EROverview]] = {
    val headers: Seq[(String, String)] = Seq(
      "Content-Type" -> "application/json",
      "srn" -> srn.id
    )

    def psrOverviewUrl = s"${config.pensionsSchemeReturnUrl}/pension-scheme-return/psr/overview/$pstr?fromDate=$startDate&toDate=$endDate"

    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.GET[HttpResponse](psrOverviewUrl)(implicitly, hc, implicitly)
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[Seq[EROverview]](Reads.seq(EROverview.rds)) match {
              case JsSuccess(data, _) =>
                data
              case JsError(errors) => throw JsResultException(errors)
            }
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }

}


