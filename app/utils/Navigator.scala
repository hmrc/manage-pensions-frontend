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

package utils

import identifiers.{Identifier, SchemeSrnId}
import models.{CheckMode, Mode, NormalMode, SchemeReferenceNumber}
import play.api.Logger
import play.api.mvc.Call

abstract class Navigator {

  private val logger = Logger(classOf[Navigator])

  protected def routeMap(ua: UserAnswers, srn: SchemeReferenceNumber): PartialFunction[Identifier, Call]

  protected def editRouteMap(ua: UserAnswers, srn: SchemeReferenceNumber): PartialFunction[Identifier, Call]

  def nextPage(id: Identifier, mode: Mode, userAnswers: UserAnswers): Call = {
    val navigateTo = {
      val srn = userAnswers.get(SchemeSrnId).getOrElse("")
      mode match {
        case NormalMode => routeMap(userAnswers, srn).lift
        case CheckMode  => editRouteMap(userAnswers, srn).lift
      }
    }

    navigateTo(id).getOrElse(defaultPage(id, mode))
  }

  private[this] def defaultPage(id: Identifier, mode: Mode): Call = {
    logger.error(s"No navigation defined for id $id in mode $mode")
    controllers.routes.IndexController.onPageLoad
  }
}
