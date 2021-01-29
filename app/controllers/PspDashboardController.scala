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

package controllers

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.actions._
import identifiers.PSPNameId
import javax.inject.Inject
import models.AuthEntity.PSP
import models.Link
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Action}
import play.twirl.api.Html
import services.PspDashboardService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.Message
import views.html.schemesOverview

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PspDashboardController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        service: PspDashboardService,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        userAnswersCacheConnector: UserAnswersCacheConnector,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: schemesOverview,
                                        config: FrontendAppConfig
                                      )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authenticate(PSP) andThen getData).async {
    implicit request =>
      val pspId: String = request.pspIdOrException.id
      val subHeading: String = Message("messages__pspDashboard__sub_heading")

      def returnLink: Option[Link] = if (request.psaId.nonEmpty) Some(link) else None

      service.getPspDetails(pspId).flatMap { details =>
        if (details.rlsFlag) {
          Future.successful(Redirect(config.pspUpdateContactDetailsUrl))
        } else {
          userAnswersCacheConnector.save(
            cacheId = request.externalId,
            id = PSPNameId,
            value = details.name
          ).map { _ =>
            Ok(view(
              name = details.name,
              cards = service.getTiles(pspId, details),
              Html(""),
              subHeading = Some(subHeading),
              returnLink = returnLink
            ))
          }
        }

      }
  }

  def link: Link = Link(
    id = "switch-psa",
    url = routes.SchemesOverviewController.onPageLoad().url,
    linkText = Message("messages__pspDashboard__switch_psa")
  )

}
