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

package config

import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Lang

class FrontEndAppConfigSpec extends PlaySpec with Matchers with GuiceOneAppPerSuite {

  implicit lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "FrontEndAppConfig" should {

    "have assets prefix" in {
      appConfig.appName must be("manage-pensions-frontend")
    }
    "have report a problem partial URL" in {
      appConfig.contactHmrcUrl must be("https://www.gov.uk/government/organisations/hm-revenue-customs/contact/pension-scheme-enquiries")
    }

    "have report a problem non-JS URL" in {
      appConfig.reportAProblemNonJSUrl must be("http://localhost:9250/contact/problem_reports_nonjs?service=PODS")
    }

    "have pspAuthEmailCallback" in {
      appConfig.pspAuthEmailCallback("psa", "psp", "pstr", "email.com") must
        be("http://localhost:8209/pension-practitioner/email-response-psp-auth/psa/psp/pstr/email.com")
    }

    "have pspDeauthEmailCallback" in {
      appConfig.pspDeauthEmailCallback("psa", "psp", "pstr", "email.com") must
        be("http://localhost:8209/pension-practitioner/email-response-psp-deauth/psa/psp/pstr/email.com")
    }

    "have pspSelfDeauthEmailCallback" in {
      appConfig.pspSelfDeauthEmailCallback("psa", "pstr", "email.com") must
        be("http://localhost:8209/pension-practitioner/email-response-psp-self-deauth/psa/pstr/email.com")
    }
    
    "have routeToSwitchLanguage" in {
      appConfig.routeToSwitchLanguage("aft").url must
        be("/manage-pension-schemes/language/aft")
    }

    "have languageMap" in {

      appConfig.languageMap must
        be(Map(
          "english" -> Lang("en"),
          "cymraeg" -> Lang("cy")))
    }

  }

}