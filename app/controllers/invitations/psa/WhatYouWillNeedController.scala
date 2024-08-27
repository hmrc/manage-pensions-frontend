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

package controllers.invitations.psa

import com.google.inject.Inject
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaSchemeAuthAction}
import controllers.invitations.psa.routes._
import controllers.psa.routes._
import identifiers.SchemeNameId
import models.{NormalMode, SchemeReferenceNumber}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.invitations.psa.whatYouWillNeed

import scala.concurrent.Future

class WhatYouWillNeedController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: whatYouWillNeed,
                                           psaSchemeAction: PsaSchemeAuthAction
                                         )
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] =
    (authenticate() andThen getData andThen psaSchemeAction(srn) andThen requireData).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          case schemeName =>
            val returnCall = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))
            Future.successful(Ok(view(schemeName, srn, returnCall)))
        }
    }

  def onSubmit(srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAction(srn)).async {
    Future.successful(Redirect(PsaNameController.onPageLoad(NormalMode, srn)))
  }
}
