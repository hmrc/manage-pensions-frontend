/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.{DataCacheConnector, ManagePensionsCacheConnector, MicroserviceCacheConnector, MongoCacheConnector}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment, Logger}

class DataCacheModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {

    configuration.getString("journey-cache") match {
      case Some("public") =>
        Seq(bind[DataCacheConnector].to[MongoCacheConnector])
      case Some("protected") =>
        Seq(
          bind[DataCacheConnector].to[ManagePensionsCacheConnector]
        )
      case _ =>
        Logger.warn("No journey-cache set, defaulting to `protected`")
        Seq(
          bind[DataCacheConnector].to[ManagePensionsCacheConnector]
        )
    }
  }
}
