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
import controllers.psp.routes._
import controllers.routes.{ContactHMRCController, SchemesOverviewController, SessionExpiredController}
import identifiers.AdministratorOrPractitionerId
import identifiers.psa.PSANameId
import models.AdministratorOrPractitioner.Administrator
import models.AuthEntity.PSP
import models.Link
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SchemesOverviewService
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers
import utils.annotations.SessionDataCache
import viewmodels.Message
import views.html.schemesOverview

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemesOverviewController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           service: SchemesOverviewService,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           userAnswersCacheConnector: UserAnswersCacheConnector,
                                           @SessionDataCache sessionDataCacheConnector: UserAnswersCacheConnector,
                                           val controllerComponents: MessagesControllerComponents,
                                           config: FrontendAppConfig,
                                           view: schemesOverview
                                         )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authenticate() andThen getData).async {
    implicit request =>
      val psaId = request.psaIdOrException.id
      service.getPsaMinimalDetails(psaId).flatMap { minDetails =>
        if (minDetails.deceasedFlag) {
          Future.successful(Redirect(ContactHMRCController.onPageLoad()))
        } else if (minDetails.rlsFlag) {
          Future.successful(Redirect(config.psaUpdateContactDetailsUrl))
        } else {
          service.getPsaName(psaId).flatMap {
            case Some(name) =>
              for {
                cards <- service.getTiles(psaId)
                penaltiesHtml <- service.retrievePenaltiesUrlPartial
                _ <- userAnswersCacheConnector.save(request.externalId, PSANameId, name)
              } yield {
                Ok(view(name, "site.psa", cards, penaltiesHtml, None, returnLink(request.pspId)))
              }
            case _ =>
              Future.successful(Redirect(SessionExpiredController.onPageLoad()))
          }
        }
      }
  }

  def redirect: Action[AnyContent] =
    Action.async(Future.successful(Redirect(SchemesOverviewController.onPageLoad())))

  def changeRoleToPsaAndLoadPage: Action[AnyContent] = (authenticate(PSP) andThen getData).async {
    implicit request =>
      sessionDataCacheConnector.fetch(request.externalId).flatMap { optionJsValue =>
        optionJsValue.map(UserAnswers).getOrElse(UserAnswers()).set(AdministratorOrPractitionerId)(Administrator).asOpt
          .fold(Future.successful(Redirect(SessionExpiredController.onPageLoad()))) { updatedUA =>
            sessionDataCacheConnector.upsert(request.externalId, updatedUA.json).map { _ =>
              Redirect(SchemesOverviewController.onPageLoad())
            }
          }
      }
  }


  private def returnLink(pspId: Option[PspId]): Option[Link] =
    if (pspId.nonEmpty) {
      Some(Link("switch-psp", PspDashboardController.changeRoleToPspAndLoadPage().url,
        Message("messages__schemeOverview__switch_psp")))
    } else {
      None
    }

}
