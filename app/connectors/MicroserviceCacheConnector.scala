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
import identifiers.TypedIdentifier
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class MicroserviceCacheConnector @Inject()(
                                            config: FrontendAppConfig,
                                            httpClientV2: HttpClientV2
                                          ) extends UserAnswersCacheConnector {

  protected def url(id: String) =
    url"${config.pensionsSchemeUrl}/pensions-scheme/journey-cache/scheme/$id"

  override def save[A, I <: TypedIdentifier[A]](
                                                 cacheId: String,
                                                 id: I,
                                                 value: A
                                               )(
                                                 implicit fmt: Format[A],
                                                 ec: ExecutionContext,
                                                 hc: HeaderCarrier
                                               ): Future[JsValue] =
    modify(cacheId, _.set(id)(value))

  override def upsert(
                       cacheId: String,
                       value: JsValue
                     )(
                       implicit ec: ExecutionContext,
                       hc: HeaderCarrier
                     ): Future[JsValue] =
    modify(cacheId, _ => JsSuccess(UserAnswers(value)))

  def remove[I <: TypedIdentifier[_]](
                                       cacheId: String,
                                       id: I
                                     )(
                                       implicit ec: ExecutionContext,
                                       hc: HeaderCarrier
                                     ): Future[JsValue] =
    modify(cacheId, _.remove(id))

  private[connectors] def modify(
                                  cacheId: String,
                                  modification: UserAnswers => JsResult[UserAnswers]
                                )(
                                  implicit ec: ExecutionContext,
                                  hc: HeaderCarrier
                                ): Future[JsValue] =
    fetch(cacheId).flatMap {
      json =>
        modification(UserAnswers(json.getOrElse(Json.obj()))) match {
          case JsSuccess(UserAnswers(updatedJson), _) =>
            httpClientV2.post(url(cacheId))
              .setHeader(CacheConnector.headers(hc): _*)
              .withBody(updatedJson)
              .execute[HttpResponse].flatMap {
                response =>
                  response.status match {
                    case OK =>
                      Future.successful(updatedJson)
                    case _ =>
                      Future.failed(new HttpException(response.body, response.status))
                  }
              }

          case JsError(errors) =>
            Future.failed(JsResultException(errors))
        }
    }

  override def fetch(id: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[JsValue]] = {
    httpClientV2.get(url(id))
      .setHeader(CacheConnector.headers(hc): _*)
      .execute[HttpResponse].flatMap {
        response =>
          response.status match {
            case NOT_FOUND =>
              Future.successful(None)
            case OK =>
              Future.successful(Some(Json.parse(response.body)))
            case _ =>
              Future.failed(new HttpException(response.body, response.status))
          }
      }
  }

  override def removeAll(id: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] =
    httpClientV2.delete(url(id))
      .setHeader(CacheConnector.headers(hc): _*)
      .execute[HttpResponse].map(_ => Ok)

}
