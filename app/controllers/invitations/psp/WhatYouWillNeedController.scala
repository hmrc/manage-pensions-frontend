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

package controllers.invitations.psp

import com.google.inject.Inject
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaPspSchemeAuthAction}
import controllers.invitations.psp.routes.PspNameController
import controllers.psa.routes._
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{NormalMode, SchemeReferenceNumber}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.invitations.psp.whatYouWillNeed

import scala.concurrent.Future

class WhatYouWillNeedController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: whatYouWillNeed,
                                           psaPspSchemeAuthAction: PsaPspSchemeAuthAction
                                         ) extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(): Action[AnyContent] = (authenticate() andThen getData andThen psaPspSchemeAuthAction(None) andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId).retrieve.map {
        case srn ~ schemeName =>
          val returnCall = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))
          Future.successful(Ok(view(schemeName, returnCall)))
      }
  }

  def onSubmit(): Action[AnyContent] = authenticate().async {
    Future.successful(Redirect(PspNameController.onPageLoad(NormalMode)))
  }

}
