/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.psp

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.actions._
import controllers.psp.routes._
import controllers.routes._
import identifiers.AdministratorOrPractitionerId
import identifiers.psp.PSPNameId
import models.AdministratorOrPractitioner.Practitioner
import models.AuthEntity.{PSA, PSP}
import models.Link
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PspDashboardService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers
import utils.annotations.SessionDataCache
import viewmodels.Message
import views.html.pspDashboard

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PspDashboardController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        service: PspDashboardService,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        userAnswersCacheConnector: UserAnswersCacheConnector,
                                        @SessionDataCache sessionDataCacheConnector: UserAnswersCacheConnector,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: pspDashboard,
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
        if (details.deceasedFlag) {
          Future.successful(Redirect(ContactHMRCController.onPageLoad()))
        } else if (details.rlsFlag) {
          Future.successful(Redirect(config.pspUpdateContactDetailsUrl))
        } else {
          userAnswersCacheConnector.save(
            cacheId = request.externalId,
            id = PSPNameId,
            value = details.name
          ).map { _ =>
            Ok(view(details.name, "site.psp", service.getTiles(pspId, details), Some(subHeading), returnLink))
          }
        }

      }
  }

  def changeRoleToPspAndLoadPage: Action[AnyContent] = (authenticate(PSA) andThen getData).async {
    implicit request =>
      sessionDataCacheConnector.fetch(request.externalId).flatMap { optionJsValue =>
        optionJsValue.map(UserAnswers).getOrElse(UserAnswers()).set(AdministratorOrPractitionerId)(Practitioner).asOpt
          .fold(Future.successful(Redirect(SessionExpiredController.onPageLoad()))) { updatedUA =>
            sessionDataCacheConnector.upsert(request.externalId, updatedUA.json).map { _ =>
              Redirect(PspDashboardController.onPageLoad())
            }
          }
      }
  }

  private def link: Link = Link("switch-psa", SchemesOverviewController.changeRoleToPsaAndLoadPage().url, Message("messages__pspDashboard__switch_psa"))
}
