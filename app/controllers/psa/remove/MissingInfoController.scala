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

package controllers.psa.remove

import controllers.actions.AuthAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.psa.remove.missingInfo

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MissingInfoController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: missingInfo
                                         )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  import MissingInfoController._

  def onPageLoad(list: List[String]): Action[AnyContent] = authenticate().async {
    implicit request => Future.successful(Ok(view(list)))
  }

  def onPageLoadPstr: Action[AnyContent] = onPageLoad(pstrKeys)
  def onPageLoadOther: Action[AnyContent] = onPageLoad(otherKey)
}

object MissingInfoController {
  private val pstrKeys  = List("messages__missing__pstr__p1", "messages__missing__pstr__p2")
  private val otherKey  = List("messages__missing__information__p1")
}
