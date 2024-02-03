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

import connectors.UserAnswersCacheConnector
import controllers.actions.{AuthAction, DataRetrievalAction, PsaSchemeAuthAction}
import models.{Individual, Organization}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.RemovalViewModel
import views.html.psa.remove.cannot_be_removed

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CanNotBeRemovedController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           userAnswersCacheConnector: UserAnswersCacheConnector,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: cannot_be_removed,
                                           getData: DataRetrievalAction,
                                           psaSchemeAuthAction: PsaSchemeAuthAction
                                         )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoadWhereSuspended: Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(None)).async {
    implicit request =>
      userAnswersCacheConnector.removeAll(request.externalId).map { _ =>
        request.userType match {
          case Individual =>
            Ok(view(viewModelIndividual))
          case Organization =>
            Ok(view(viewModelOrganisation))
          case _ =>
            Redirect(controllers.routes.SessionExpiredController.onPageLoad)
        }
      }
  }

  def onPageLoadWhereRemovalDelay: Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(None)).async {
    implicit request =>
      Future.successful(Ok(view(viewModelRemovalDelay)))
  }

  private def viewModelIndividual: RemovalViewModel = RemovalViewModel(
    "messages__you_cannot_be_removed__title",
    "messages__you_cannot_be_removed__heading",
    "messages__you_cannot_be_removed__p1",
    "messages__you_cannot_be_removed__p2",
    "messages__you_cannot_be_removed__returnToSchemes__link")

  private def viewModelOrganisation: RemovalViewModel = RemovalViewModel(
    "messages__psa_cannot_be_removed__title",
    "messages__psa_cannot_be_removed__heading",
    "messages__psa_cannot_be_removed__p1",
    "messages__psa_cannot_be_removed__p2",
    "messages__psa_cannot_be_removed__returnToSchemes__link")

  private def viewModelRemovalDelay: RemovalViewModel = RemovalViewModel(
    "messages__psa_cannot_be_removed__title",
    "messages__psa_cannot_be_removed__heading",
    "messages__psa_cannot_be_removed_delay__p1",
    "messages__psa_cannot_be_removed_delay__p2",
    "messages__psa_cannot_be_removed__returnToSchemes__link")
}
