/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import com.google.inject.{Inject, Singleton}
import connectors.SubscriptionConnector
import controllers.actions.AuthAction
import models.{PsaSubscriptionDetails, SubscriptionDetails}
import play.api.mvc.{AnyContent, Action}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

@Singleton
class InviteController@Inject()(authenticate: AuthAction,connector:SubscriptionConnector) extends FrontendController {

  def onPageLoad: Action[AnyContent] = authenticate.async{
    implicit request =>
      connector.getSubscriptionDetails(request.psaId.id) map { subscriptionDetails =>
        subscriptionDetails.psaSubscriptionDetails.isPSASuspension match {
          case true => Redirect(controllers.routes.YouCannotSendAnInviteController.onPageLoad())
          case false => Ok
        }
      }
  }

}