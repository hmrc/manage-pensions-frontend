/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.psp.deauthorise.self

import com.google.inject.Inject
import connectors.UserAnswersCacheConnector
import connectors.admin.MinimalConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.{AuthorisedPractitionerId, SchemeNameId}
import models.AuthEntity.PSP
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.psp.deauthorisation.self.confirmation

import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(override val messagesApi: MessagesApi,
                                       auth: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       minimalConnector: MinimalConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: confirmation
                                      )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def onPageLoad(): Action[AnyContent] = (auth(PSP) andThen getData andThen requireData).async {
    implicit request =>

      (SchemeNameId and AuthorisedPractitionerId).retrieve.right.map {
        case schemeName ~ psp =>
          minimalConnector.getMinimalPspDetails(request.pspIdOrException.id) flatMap { pspDetails =>
            userAnswersCacheConnector.removeAll(request.externalId) map { _ =>
              Ok(view(schemeName, psp.authorisingPSA.name, pspDetails.email))
            }
          }
      }
  }
}
