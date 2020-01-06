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
import org.scalatest.{AsyncWordSpec, MustMatchers}

class PensionAdminCacheConnectorSpec extends AsyncWordSpec with MustMatchers with ConnectorBehaviour {

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  protected def pensionAdminCacheConnectorUrl(id: String) = s"/pension-administrator/journey-cache/psa/$id"
  protected def pensionAdminCacheConnectorLastUpdatedUrl(id: String) = s"/pension-administrator/journey-cache/psa/$id/lastUpdated"

  protected def managePensionsCacheConnectorUrl(id: String) = s"/pension-administrator/journey-cache/manage-pensions/$id"
  protected def managePensionsCacheConnectorLastUpdatedUrl(id: String) = s"/pension-administrator/journey-cache/manage-pensions/$id/lastUpdated"

  "PensionAdminCacheConnector" must {

    behave like cacheConnector[PensionAdminCacheConnector](pensionAdminCacheConnectorUrl, pensionAdminCacheConnectorLastUpdatedUrl)
  }

  "ManagePensionsCacheConnector" must {

    behave like cacheConnector[ManagePensionsCacheConnector](managePensionsCacheConnectorUrl, managePensionsCacheConnectorLastUpdatedUrl)
  }
}
