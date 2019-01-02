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

package utils

import config.FrontendAppConfig
import javax.inject.Inject
import play.api.Environment
import utils.countryOptions.CountryOptions

class FakeCountryOptions @Inject()(
                                    environment: Environment,
                                    config: FrontendAppConfig
                                  ) extends CountryOptions(environment, config) {
  override def options: Seq[InputOption] = FakeCountryOptions.fakeCountries
}

object FakeCountryOptions {

  def fakeCountries: Seq[InputOption] = {

    for {
      a <- 'A' to 'Z'
      b <- 'A' to 'Z'
    } yield {
      val country: String = Seq(a, b).mkString("")
      InputOption(country, s"Country of $country", None)
    }

  }

}
