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

package controllers

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.actions._
import identifiers.PSANameId
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import services.SchemesOverviewService
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.schemesOverview

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SchemesOverviewController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           service: SchemesOverviewService,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           userAnswersCacheConnector: UserAnswersCacheConnector,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: schemesOverview
                                         )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad: Action[AnyContent] = (authenticate() andThen getData).async {
    implicit request =>

      val psaId = request.psaIdOrException.id

      service.getPsaName(psaId).flatMap {

        case Some(name) =>

          service.getTiles(psaId).flatMap { cards =>
            userAnswersCacheConnector.save(request.externalId, PSANameId, name).map { _ =>
              Ok(view(name, cards, None))
            }
          }

        case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  def redirect: Action[AnyContent] = Action.async(Future.successful(Redirect(controllers.routes.SchemesOverviewController.onPageLoad())))

}
