/*
 * Copyright 2019 HM Revenue & Customs
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

class PensionAdministratorFeatureSwitchConnectorImpl @Inject()(http: HttpClient, appConfig: FrontendAppConfig) extends FeatureSwitchConnector {

  override def toggleOn(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    val url = appConfig.pensionAdminUrl + s"/pension-administrator/test-only/toggle-on/$name"

    http.GET(url).map { _ =>
      true
    }.recoverWith {
      case _ =>
        Future.successful(false)
    }
  }

  override def toggleOff(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    val url = appConfig.pensionAdminUrl + s"/pension-administrator/test-only/toggle-off/$name"

    http.GET(url).map { _ =>
      true
    }.recoverWith {
      case _ =>
        Future.successful(false)
    }
  }
  override def reset(name: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val url = appConfig.pensionAdminUrl + s"/pension-administrator/test-only/reset/$name"

    http.GET(url).map { _ =>
      true
    }.recoverWith {
      case _ =>
        Future.successful(false)
    }
  }
}


