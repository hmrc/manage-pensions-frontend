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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.FrontendAppConfig
import models.ListOfLegacySchemes
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MigrationConnectorImpl])
trait MigrationConnector {

  def getListOfLegacySchemes(psaId: String)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpResponse, ListOfLegacySchemes]]
}

@Singleton
class MigrationConnectorImpl @Inject()(httpClientV2: HttpClientV2, config: FrontendAppConfig) extends MigrationConnector {

  private val logger = Logger(classOf[MigrationConnectorImpl])
  private val listOfLegacySchemesUrl = url"${config.migrationListOfLegacySchemesApiUrl}"

  def getListOfLegacySchemes(psaId: String)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpResponse, ListOfLegacySchemes]] = {
    val migrationHc = hc.withExtraHeaders("psaId" -> psaId)

    httpClientV2.get(listOfLegacySchemesUrl)(using migrationHc)
      .setHeader("psaId" -> psaId)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[ListOfLegacySchemes] match {
              case JsSuccess(value, _) => Right(value)
              case JsError(errors) => throw JsResultException(errors)
            }
          case _ =>
            logger.error(response.body)
            Left(response)
        }
      }
  }
}
