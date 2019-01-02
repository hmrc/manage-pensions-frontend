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

package toggles

import config.FrontendAppConfig
import org.scalatest.{Matchers, WordSpec}
import play.api.{Configuration, Environment}

class FeatureToggleBehaviours extends WordSpec with Matchers {

  import FeatureToggleBehaviours._

  def featureToggle(name: String, getter: FrontendAppConfig => Boolean): Unit = {

    "behave like a feature toggle" should {

      s"return true when $name is configured as true" in {
        getter(configuration(name, Some(true))) shouldBe true
      }

      s"return false when $name is configured as false" in {
        getter(configuration(name, Some(false))) shouldBe false
      }

      s"return false when $name is not configured" in {
        getter(configuration(name, None)) shouldBe false
      }

    }

  }

}

object FeatureToggleBehaviours {

  def configuration(name: String, on: Option[Boolean]): FrontendAppConfig = {

    new FrontendAppConfig(
      on.fold {
        Configuration()
      } {
        b => Configuration(s"features.$name" -> b.toString)
      },
      Environment.simple()
    )

  }

}
