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

package controllers.invitations.psp

import controllers.Retrievals
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import identifiers.SchemeNameId
import identifiers.SchemeSrnId
import identifiers.invitations.psp.PspNameId
import javax.inject.Inject
import models.SchemeReferenceNumber
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.invitations.psp.pspDoesNotMatch

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PspDoesNotMatchController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: pspDoesNotMatch
                                           )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and SchemeSrnId and PspNameId).retrieve.right.map {
        case schemeName ~ srn ~ pspName =>
           Future.successful(Ok(view(schemeName, pspName, returnCall(srn))))
      }
  }

  private def returnCall(srn:String):Call  = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber(srn))

}
