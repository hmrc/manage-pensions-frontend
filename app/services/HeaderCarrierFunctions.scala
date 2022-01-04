/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartials, HeaderCarrierForPartialsConverter}

object HeaderCarrierFunctions extends FrontendHeaderCarrierProvider {

  def headerCarrierForPartials[A](request: Request[A]): HeaderCarrierForPartials =
    HCForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
}

object HCForPartialsConverter extends HeaderCarrierForPartialsConverter with SessionCookieCryptoFilterWrapper {
  val crypto: String => String = encryptCookieString
}

trait SessionCookieCryptoFilterWrapper {
  def encryptCookieString(cookie: String): String = cookie
}
