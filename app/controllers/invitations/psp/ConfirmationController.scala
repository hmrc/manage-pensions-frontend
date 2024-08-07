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

import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaSchemeAuthAction}
import identifiers.SchemeNameId
import identifiers.invitations.psp.PspNameId
import models.SchemeReferenceNumber
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.invitations.psp.confirmation

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        userAnswersCacheConnector: UserAnswersCacheConnector,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: confirmation,
                                        psaSchemeAuthAction: PsaSchemeAuthAction
                                      )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeNameId and PspNameId).retrieve.map {
        case schemeName ~ pspName =>
          userAnswersCacheConnector.removeAll(request.externalId).map { _ =>
            Ok(view(schemeName, pspName))
          }
      }
  }
}
