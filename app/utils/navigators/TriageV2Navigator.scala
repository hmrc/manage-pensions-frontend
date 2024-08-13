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

package utils.navigators

import config.FrontendAppConfig
import controllers.triagev2.routes._
import identifiers.Identifier
import identifiers.triagev2._
import models.SchemeReferenceNumber
import models.triagev2.WhatDoYouWantToDo.{FileAccountingForTaxReturn, FileEventReport, FilePensionSchemeReturn, ManageExistingScheme}
import models.triagev2.WhatRole.{PSA, PSP}
import models.triagev2.WhichServiceYouWantToView.{IamUnsure, ManagingPensionSchemes, PensionSchemesOnline}
import models.triagev2._
import play.api.mvc.Call
import utils.{Enumerable, Navigator, UserAnswers}

import javax.inject.{Inject, Singleton}

@Singleton
class TriageV2Navigator @Inject()(appConfig: FrontendAppConfig) extends Navigator with Enumerable.Implicits {

  override def routeMap(ua: UserAnswers, srn: SchemeReferenceNumber): PartialFunction[Identifier, Call] = {

    case WhatRoleId => whatRoleRoutes(ua)
    case WhichServiceYouWantToViewId => whichServiceYouWantToViewRoutes(ua)
    case WhatDoYouWantToDoId => whatDoYouWantToDoRoutes(ua)
  }

  private def whatRoleRoutes(ua: UserAnswers): Call =
    ua.get(WhatRoleId) match {
      case role@(Some(PSA) | Some(PSP)) => WhichServiceYouWantToViewController.onPageLoad(role.get.toString)
      case _ => controllers.triagev2.routes.NotRegisteredController.onPageLoad
    }

  private def whichServiceYouWantToViewRoutes(ua: UserAnswers): Call = {

    ua.get(WhatRoleId) match {
      case role@(Some(PSA) | Some(PSP)) => ua.get(WhichServiceYouWantToViewId)(reads(WhichServiceYouWantToView.enumerable(role.get.toString))) match {
        case Some(ManagingPensionSchemes) => Call("GET", s"${appConfig.loginUrl}?continue=${
          role.get.toString match {
            case "administrator" => appConfig.psaOverviewUrl
            case _ => appConfig.pspDashboardUrl
          }
        }")
        case Some(PensionSchemesOnline) => Call("GET", appConfig.tpssWelcomeUrl)
        case Some(IamUnsure) => WhatDoYouWantToDoController.onPageLoad(role.get.toString)
        case _ => controllers.routes.SessionExpiredController.onPageLoad
      }
      case _ => controllers.routes.SessionExpiredController.onPageLoad
    }
  }

  private def whatDoYouWantToDoRoutes(ua: UserAnswers): Call =
    ua.get(WhatRoleId) match {
      case role@(Some(PSA) | Some(PSP)) =>
        val memberRole = role.get.toString
        ua.get(WhatDoYouWantToDoId)(reads(WhatDoYouWantToDo.enumerable(memberRole))) match {
          case Some(ManageExistingScheme) => ManageExistingSchemeController.onPageLoad(memberRole)
          case Some(FileAccountingForTaxReturn) => FileAccountingForTaxReturnController.onPageLoad(memberRole)
          case Some(FilePensionSchemeReturn) => FilePensionSchemeReturnController.onPageLoad(memberRole)
          case Some(FileEventReport) => FileEventReportController.onPageLoad(memberRole)
          case _ => controllers.routes.SessionExpiredController.onPageLoad
        }
      case _ => controllers.routes.SessionExpiredController.onPageLoad
    }

  override protected def editRouteMap(ua: UserAnswers, srn: SchemeReferenceNumber): PartialFunction[Identifier, Call] = {
    case _ => controllers.routes.SessionExpiredController.onPageLoad
  }
}
