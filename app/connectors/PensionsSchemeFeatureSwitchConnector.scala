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

package connectors

import com.google.inject.Inject
import config.FrontendAppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

trait FeatureSwitchConnector {
  def toggleOn(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean]

  def toggleOff(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean]

  def reset(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean]

  def get(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]]
}

class PensionsSchemeFeatureSwitchConnectorImpl @Inject()(http: HttpClient, appConfig: FrontendAppConfig) extends FeatureSwitchConnector {

  override def toggleOn(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    val url = appConfig.pensionsSchemeUrl + s"/pensions-scheme/test-only/toggle-on/$name"

    http.GET(url).map { _ =>
      true
    }.recoverWith {
      case _ =>
        Future.successful(false)
    }
  }

  override def toggleOff(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    val url = appConfig.pensionsSchemeUrl + s"/pensions-scheme/test-only/toggle-off/$name"

    http.GET(url).map { _ =>
      true
    }.recoverWith {
      case _ =>
        Future.successful(false)
    }
  }

  override def reset(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val url = appConfig.pensionsSchemeUrl + s"/pensions-scheme/test-only/reset/$name"

    http.GET(url).map { _ =>
      true
    }.recoverWith {
      case _ =>
        Future.successful(false)
    }
  }

  override def get(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] = {
    val url = appConfig.pensionsSchemeUrl + s"/pensions-scheme/test-only/get/$name"

    http.GET(url).map { value =>
      val currentValue = value.json.as[Boolean]
      Option(currentValue)
    }.recoverWith {
      case _ =>
        Future.successful(None)
    }
  }
}


