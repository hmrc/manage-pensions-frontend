/*
 * Copyright 2021 HM Revenue & Customs
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

import identifiers.Identifier
import models.{Mode, NormalMode, CheckMode}
import play.api.Logger
import play.api.mvc.Call

abstract class Navigator {

  private val logger = Logger(classOf[Navigator])

  protected def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call]

  protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call]

  def nextPage(id: Identifier, mode: Mode, userAnswers: UserAnswers): Call = {
    val navigateTo = {
      mode match {
        case NormalMode => routeMap(userAnswers).lift
        case CheckMode => editRouteMap(userAnswers).lift
      }
    }

    navigateTo(id).getOrElse(defaultPage(id, mode))
  }

  private[this] def defaultPage(id: Identifier, mode: Mode): Call = {
    logger.warn(s"No navigation defined for id $id in mode $mode")
    controllers.routes.IndexController.onPageLoad()
  }
}
