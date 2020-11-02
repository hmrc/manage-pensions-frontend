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

import connectors.UserAnswersCacheConnector
import controllers.actions._
import identifiers.PSPNameId
import javax.inject.Inject
import models.AuthEntity.PSP
import models.Link
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PspDashboardService
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import viewmodels.Message
import views.html.schemesOverview

import scala.concurrent.{ExecutionContext, Future}

class PspDashboardController @Inject()(override val messagesApi: MessagesApi,
                                       service: PspDashboardService,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: schemesOverview)
                                      (implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  def onPageLoad: Action[AnyContent] = (authenticate(PSP) andThen getData).async {
    implicit request =>
      val pspId: String = request.pspIdOrException.id
      val subHeading: String = Message("messages__pspDashboard__sub_heading")
      def returnLink: Option[Link] = if (request.psaId.nonEmpty) Some(link) else None

      service.getPspName(pspId).flatMap {
        case Some(name) =>
          userAnswersCacheConnector.save(request.externalId, PSPNameId, name).map { _ =>
            Ok(view(name, service.getTiles(pspId), Some(subHeading), returnLink))
          }

        case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  def link: Link = Link("switch-psa", routes.SchemesOverviewController.onPageLoad().url,
    Message("messages__pspDashboard__switch_psa"))

}