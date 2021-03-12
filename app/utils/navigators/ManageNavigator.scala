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

package utils.navigators

import controllers.routes._
import identifiers.{Identifier, AdministratorOrPractitionerId}
import models.AdministratorOrPractitioner.{Practitioner, Administrator}

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import utils.{UserAnswers, Navigator, Enumerable}

@Singleton
class ManageNavigator @Inject()() extends Navigator with Enumerable.Implicits {

  override def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
    case AdministratorOrPractitionerId =>
      ua.get(AdministratorOrPractitionerId) match {
        case Some(Administrator) => controllers.routes.SchemesOverviewController.onPageLoad()
        case Some(Practitioner) => controllers.routes.PspDashboardController.onPageLoad()
        case _ => SessionExpiredController.onPageLoad()
      }
  }

  override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = routeMap(ua)
}
