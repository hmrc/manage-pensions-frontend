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

package controllers.invitations

import controllers.actions._
import controllers.behaviours.ControllerWithNormalPageBehaviours
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.invitations.youCannotSendAnInvite

class YouCannotSendAnInviteControllerSpec extends ControllerWithNormalPageBehaviours {

  private val youCannotSendAnInviteView = injector.instanceOf[youCannotSendAnInvite]

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new YouCannotSendAnInviteController(
      frontendAppConfig, messagesApi, fakeAuth, dataRetrievalAction, requiredDateAction,
      stubMessagesControllerComponents(), youCannotSendAnInviteView).onPageLoad()
  }

  private def viewAsString(): String = youCannotSendAnInviteView()(fakeRequest, messages).toString

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, None, viewAsString)
}
