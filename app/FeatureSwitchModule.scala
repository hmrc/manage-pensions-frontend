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

import config.FeatureSwitchManagementService
import config.FeatureSwitchManagementServiceProductionImpl
import config.FeatureSwitchManagementServiceTestImpl
import play.api.inject.Binding
import play.api.inject.Module
import play.api.Configuration
import play.api.Environment

class FeatureSwitchModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {

    if(configuration.underlying.getBoolean("enable-dynamic-switches")){
      Seq(
        bind[FeatureSwitchManagementService].to[FeatureSwitchManagementServiceTestImpl]
      )
    } else {
      Seq(
        bind[FeatureSwitchManagementService].to[FeatureSwitchManagementServiceProductionImpl]
      )
    }
  }
}
