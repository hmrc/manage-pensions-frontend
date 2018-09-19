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

package utils

import connectors.{DataCacheConnector, FakeDataCacheConnector}
import identifiers.Identifier
import models.requests.IdentifiedRequest
import models.{Mode, NormalMode}
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class FakeNavigator(val desiredRoute: Call, mode: Mode = NormalMode) extends Navigator {

  private[this] var userAnswers: Option[UserAnswers] = None

  def lastUserAnswers: Option[UserAnswers] = userAnswers

  override protected def dataCacheConnector: DataCacheConnector = FakeDataCacheConnector

  override def nextPage(id: Identifier, mode: Mode, answers: UserAnswers)
                       (implicit ex: IdentifiedRequest, ec: ExecutionContext, hc: HeaderCarrier): Call = {
    userAnswers = Some(answers)
    desiredRoute
  }

  override protected def routeMap(from: NavigateFrom): Option[NavigateTo] = None

  override protected def editRouteMap(from: NavigateFrom): Option[NavigateTo] = None

}

object FakeNavigator extends FakeNavigator(Call("GET", "www.example.com"), NormalMode)