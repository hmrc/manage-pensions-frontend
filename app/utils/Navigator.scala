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

import connectors.UserAnswersCacheConnector
import identifiers.{LastPageId, Identifier}
import models.requests.IdentifiedRequest
import models.{LastPage, CheckMode, Mode, NormalMode}
import play.api.Logger
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.util.Failure

abstract class Navigator {

  protected def dataCacheConnector: UserAnswersCacheConnector

  protected def routeMap(from: NavigateFrom): Option[NavigateTo]

  protected def editRouteMap(from: NavigateFrom): Option[NavigateTo]

  def nextPage(id: Identifier, mode: Mode, userAnswers: UserAnswers)(implicit ex: IdentifiedRequest, ec: ExecutionContext, hc: HeaderCarrier): Call = {
    val navigateTo = {
      mode match {
        case NormalMode => routeMap(NavigateFrom(id, userAnswers))
        case CheckMode => editRouteMap(NavigateFrom(id, userAnswers))
      }
    }

    navigateTo
      .map(to => saveAndContinue(to, ex.externalId))
      .getOrElse(defaultPage(id, mode))
  }

  private[this] def saveAndContinue(navigation: NavigateTo, externalID: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Call = {
    if (navigation.save) {
      dataCacheConnector.save(externalID, LastPageId, LastPage(navigation.page.method, navigation.page.url)) andThen {
        case Failure(t: Throwable) => Logger.warn("Error saving user's current page", t)
      }
    }
    navigation.page
  }

  private[this] def defaultPage(id: Identifier, mode: Mode): Call = {
    Logger.warn(s"No navigation defined for id $id in mode $mode")
    controllers.routes.IndexController.onPageLoad()
  }

  case class NavigateFrom(id: Identifier, userAnswers: UserAnswers)

  case class NavigateTo(page: Call, save: Boolean)

  object NavigateTo {
    def save(page: Call): Option[NavigateTo] = Some(NavigateTo(page, true))

    def dontSave(page: Call): Option[NavigateTo] = Some(NavigateTo(page, false))
  }

}
