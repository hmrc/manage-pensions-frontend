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

package controllers.remove.pspSelfRemoval

import com.google.inject.Inject
import connectors.UserAnswersCacheConnector
import connectors.admin.MinimalConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.{SchemeNameId, SeqAuthorisedPractitionerId}
import models.AuthEntity.PSP
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.remove.pspSelfRemoval.confirmation

import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(override val messagesApi: MessagesApi,
                                       auth: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       minimalConnector: MinimalConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: confirmation
                                     )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(): Action[AnyContent] = (auth(PSP) andThen getData andThen requireData).async {
      implicit request =>

        (SchemeNameId and SeqAuthorisedPractitionerId).retrieve.right.map {
          case schemeName ~ pspList =>
            val pspId: String = request.pspIdOrException.id

            minimalConnector.getMinimalPspDetails(request.pspIdOrException.id) map { pspDetails =>
         //       userAnswersCacheConnector.removeAll(request.externalId) map { _ =>
                  pspList.find(_.id == pspId).map { psp =>
                    Ok(view(schemeName, psp.authorisingPSA.name, pspDetails.email))
                  }.getOrElse {
                    Logger.debug("Logged in PSP not found in the list of PSPs for the given scheme")
                    Redirect(controllers.routes.SessionExpiredController.onPageLoad())
                  }
         //       }
            }
        }
    }
}
