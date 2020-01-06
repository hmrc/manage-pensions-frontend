/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.remove

import base.SpecBase
import connectors.FakeUserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions.{AuthAction, DataRetrievalAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers
import views.html.remove.confirmRemoved

class ConfirmRemovedControllerSpec extends ControllerWithNormalPageBehaviours {

  import ConfirmRemovedControllerSpec._

  "ConfirmRemovedController" should {

    behave like controllerWithOnPageLoadMethod(onPageLoadAction(this), getEmptyData, validData, viewAsString(this))

  }

}

object ConfirmRemovedControllerSpec extends ControllerSpecBase {
  private val testPsaName = "test-pas-name"
  private val testSchemeName = "test-scheme-name"
  private val confirmRemovedView = injector.instanceOf[confirmRemoved]

  def onPageLoadAction(base: ControllerWithNormalPageBehaviours)(dataRetrievalAction: DataRetrievalAction, authAction: AuthAction): Action[AnyContent] =
    new ConfirmRemovedController(
      base.frontendAppConfig,
      base.messagesApi,
      authAction,
      dataRetrievalAction,
      base.requiredDateAction,
      FakeUserAnswersCacheConnector,
      stubMessagesControllerComponents(),
      confirmRemovedView
    ).onPageLoad()

  val validData: Option[DataRetrievalAction] = {
    Some(
      UserAnswers()
        .psaName(testPsaName)
        .schemeName(testSchemeName)
        .dataRetrievalAction
    )
  }

  def viewAsString(base: SpecBase)(): String =
    confirmRemovedView(
      testPsaName,
      testSchemeName
    )(base.fakeRequest, base.messages).toString()

}
