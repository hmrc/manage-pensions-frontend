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

package utils.navigators

import base.SpecBase
import base.SpecBase.frontendAppConfig
import identifiers.Identifier
import identifiers.triagev2.{WhichServiceYouWantToViewId, _}
import models.triagev2.WhatDoYouWantToDo.{FileAccountingForTaxReturn, FileEventReport, FilePensionSchemeReturn, ManageExistingScheme}
import models.triagev2.WhatRole.{PSA, PSP}
import models.triagev2.WhichServiceYouWantToView.{IamUnsure, ManagingPensionSchemes, PensionSchemesOnline}
import models.triagev2.{WhatDoYouWantToDo, WhichServiceYouWantToView}
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{Enumerable, NavigatorBehaviour, UserAnswers}

class TriageV2NavigatorSpec extends SpecBase with NavigatorBehaviour {

  import TriageV2NavigatorSpec._

  private val navigator = new TriageV2Navigator(frontendAppConfig)

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id", "User Answers", "Next Page (NormalMode)", "Next Page (CheckMode)"),
    (WhichServiceYouWantToViewId, whichServiceYouWantToViewAnswersPsa(ManagingPensionSchemes), psaOverviewPage, None),
    (WhichServiceYouWantToViewId, whichServiceYouWantToViewAnswersPsp(ManagingPensionSchemes), pspOverviewPage, None),
    (WhichServiceYouWantToViewId, whichServiceYouWantToViewAnswersPsa(PensionSchemesOnline), tpssLoginPage, None),
    (WhichServiceYouWantToViewId, whichServiceYouWantToViewAnswersPsp(PensionSchemesOnline), tpssLoginPage, None),
    (WhichServiceYouWantToViewId, whichServiceYouWantToViewAnswersPsa(IamUnsure), whatDoYouWantToDoPage("PSA"), None),
    (WhichServiceYouWantToViewId, whichServiceYouWantToViewAnswersPsp(IamUnsure), whatDoYouWantToDoPage("PSP"), None),
    (WhichServiceYouWantToViewId, emptyAnswers, sessionExpiredPage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswersPsa(ManageExistingScheme), manageExistingSchemePage("PSA"), None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswersPsp(ManageExistingScheme), manageExistingSchemePage("PSP"), None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswersPsa(FileAccountingForTaxReturn), fileAccountingForTaxReturnPage("PSA"), None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswersPsp(FileAccountingForTaxReturn), fileAccountingForTaxReturnPage("PSP"), None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswersPsa(FilePensionSchemeReturn), filePensionSchemeReturnPage("PSA"), None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswersPsp(FilePensionSchemeReturn), filePensionSchemeReturnPage("PSP"), None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswersPsa(FileEventReport), fileEventReportPage("PSA"), None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswersPsp(FileEventReport), fileEventReportPage("PSP"), None),
    (WhatDoYouWantToDoId, emptyAnswers, sessionExpiredPage, None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}
  object TriageV2NavigatorSpec extends OptionValues with Enumerable.Implicits {

    lazy val emptyAnswers: UserAnswers = UserAnswers(Json.obj())
    lazy val whatRolePsaAnswers: UserAnswers = UserAnswers().set(WhatRoleId)(PSA).asOpt.value
    lazy val whatRolePspAnswers: UserAnswers = UserAnswers().set(WhatRoleId)(PSP).asOpt.value

    private def whichServiceYouWantToViewAnswersPsa(answer: WhichServiceYouWantToView): UserAnswers =
      whatRolePsaAnswers.set(WhichServiceYouWantToViewId)(answer)(writes(WhichServiceYouWantToView.enumerable("PSA"))).asOpt.value

    private def whichServiceYouWantToViewAnswersPsp(answer: WhichServiceYouWantToView): UserAnswers =
      whatRolePspAnswers.set(WhichServiceYouWantToViewId)(answer)(writes(WhichServiceYouWantToView.enumerable("PSP"))).asOpt.value

    private def whatDoYouWantToDoAnswersPsa(answer: WhatDoYouWantToDo): UserAnswers =
      whatRolePsaAnswers.set(WhatDoYouWantToDoId)(answer)(writes(WhatDoYouWantToDo.enumerable("PSA"))).asOpt.value

    private def whatDoYouWantToDoAnswersPsp(answer: WhatDoYouWantToDo): UserAnswers =
      whatRolePspAnswers.set(WhatDoYouWantToDoId)(answer)(writes(WhatDoYouWantToDo.enumerable("PSP"))).asOpt.value


    private def psaOverviewPage: Call = Call("GET", frontendAppConfig.loginUrl + "?continue="
      + frontendAppConfig.managePensionsUrl + controllers.routes.SchemesOverviewController.onPageLoad().url)

    private def pspOverviewPage: Call = Call("GET", frontendAppConfig.loginUrl + "?continue="
      + frontendAppConfig.managePensionsUrl + controllers.psp.routes.PspDashboardController.onPageLoad().url)
    private def tpssLoginPage: Call = Call("GET", frontendAppConfig.tpssWelcomeUrl)

    private def whatDoYouWantToDoPage(role:String): Call = controllers.triagev2.routes.WhatDoYouWantToDoController.onPageLoad(role)

    private def manageExistingSchemePage(role:String): Call = controllers.triagev2.routes.ManageExistingSchemeController.onPageLoad(role)
    private def fileAccountingForTaxReturnPage(role:String): Call = controllers.triagev2.routes.FileAccountingForTaxReturnController.onPageLoad(role)
    private def filePensionSchemeReturnPage(role:String): Call = controllers.triagev2.routes.FilePensionSchemeReturnController.onPageLoad(role)
    private def fileEventReportPage(role:String): Call = controllers.triagev2.routes.FileEventReportController.onPageLoad(role)

    private def sessionExpiredPage: Call = controllers.routes.SessionExpiredController.onPageLoad()


    private def dataDescriber(answers: UserAnswers): String = answers.toString
  }





