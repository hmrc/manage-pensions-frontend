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

package controllers.remove

import com.google.inject.Inject
import connectors.UserAnswersCacheConnector
import connectors.admin.MinimalConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.SchemeNameId
import identifiers.remove.PspDetailsId
import models.Index
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.remove.confirmPsaRemovedPsp

import scala.concurrent.ExecutionContext

class ConfirmPsaRemovedPspController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                userAnswersCacheConnector: UserAnswersCacheConnector,
                                                val controllerComponents: MessagesControllerComponents,
                                                minimalPsaConnector: MinimalConnector,
                                                view: confirmPsaRemovedPsp
                                              )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate() andThen getData andThen requireData).async {
      implicit request =>

        (SchemeNameId and PspDetailsId(index)).retrieve.right.map {
          case schemeName ~ pspDetails =>
            minimalPsaConnector.getMinimalPsaDetails(request.psaIdOrException.id) flatMap {
              psaDetails =>
                userAnswersCacheConnector.removeAll(request.externalId) map {
                  _ =>
                    Ok(view(pspDetails.name, schemeName, psaDetails.email))
                }
            }
        }
    }
}
