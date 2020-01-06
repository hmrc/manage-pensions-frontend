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
import javax.inject.Singleton
import play.api.libs.ws.WSClient

@Singleton
class UpdateSchemeCacheConnector @Inject()(
                                            config: FrontendAppConfig,
                                            http: WSClient
                                          ) extends MicroserviceCacheConnector(config, http) {

  override protected def url(id: String) = s"${config.pensionsSchemeUrl}/pensions-scheme/journey-cache/update-scheme/$id"

  override protected def lastUpdatedUrl(id: String) = s"${config.pensionsSchemeUrl}/pensions-scheme/journey-cache/update-scheme/$id/lastUpdated"
}
