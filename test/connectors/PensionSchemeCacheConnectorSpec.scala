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

import connectors.behaviour.ConnectorBehaviour
import connectors.scheme.{SchemeSubscriptionCacheConnector, UpdateSchemeCacheConnector}
import org.scalatest.{AsyncWordSpec, MustMatchers}

class PensionSchemeCacheConnectorSpec extends AsyncWordSpec with MustMatchers with ConnectorBehaviour {

  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  protected def userAnswersCacheConnectorUrl(id: String): String = s"/pensions-scheme/journey-cache/scheme/$id"
  protected def userAnswersCacheConnectorLastUpdatedUrl(id: String) = s"/pensions-scheme/journey-cache/scheme/$id/lastUpdated"

  protected def updateSchemeCacheConnectorUrl(id: String): String = s"/pensions-scheme/journey-cache/update-scheme/$id"
  protected def updateSchemeCacheConnectorLastUpdatedUrl(id: String) = s"/pensions-scheme/journey-cache/update-scheme/$id/lastUpdated"

  protected def schemeSubscriptionCacheConnectorUrl(id: String): String = s"/pensions-scheme/journey-cache/scheme-subscription/$id"
  protected def schemeSubscriptionCacheConnectorLastUpdatedUrl(id: String) = s"/pensions-scheme/journey-cache/scheme-subscription/$id/lastUpdated"

  "MicroserviceCacheConnector" must {

    behave like cacheConnector[MicroserviceCacheConnector](userAnswersCacheConnectorUrl, userAnswersCacheConnectorLastUpdatedUrl)
  }

  "UpdateSchemeCacheConnector" must {

    behave like cacheConnector[UpdateSchemeCacheConnector](updateSchemeCacheConnectorUrl, updateSchemeCacheConnectorLastUpdatedUrl)
  }

  "SchemeSubscriptionCacheConnector" must {

    behave like cacheConnector[SchemeSubscriptionCacheConnector](schemeSubscriptionCacheConnectorUrl, schemeSubscriptionCacheConnectorLastUpdatedUrl)
  }
}
