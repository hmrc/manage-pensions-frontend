/*
 * Copyright 2019 HM Revenue & Customs
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
import connectors.{MinimalPsaConnector, PensionSchemeVarianceLockConnector, UpdateSchemeCacheConnector, UserAnswersCacheConnector}
import controllers.actions._
import javax.inject.Inject
import models._
import models.requests.OptionalDataRequest
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, Result}
import services.SchemesOverviewService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.PensionsSchemeCache
import viewmodels.{CardViewModel, Message}
import views.html.schemesOverview

import scala.concurrent.{ExecutionContext, Future}

class SchemesOverviewController @Inject()(appConfig: FrontendAppConfig,
                                          override val messagesApi: MessagesApi,
                                          @PensionsSchemeCache dataCacheConnector: UserAnswersCacheConnector,
                                          minimalPsaConnector: MinimalPsaConnector,
                                          service: SchemesOverviewService,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction
                                         )
                                         (implicit val ec: ExecutionContext) extends FrontendController with I18nSupport {



  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

          val psaId = request.psaId.id
            minimalPsaConnector.getPsaNameFromPsaID(psaId).flatMap { psaName =>

              service.getTiles(psaId).map { cards =>

                  Ok(schemesOverview(appConfig, psaName.getOrElse("Psa name not found"), cards))
                }
              }
            }



  def onClickCheckIfSchemeCanBeRegistered: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      for {
        data <- dataCacheConnector.fetch(request.externalId)
        psaMinimalDetails <- minimalPsaConnector.getMinimalPsaDetails(request.psaId.id)
        result <- service.retrieveResult(data, Some(psaMinimalDetails))
      } yield {
        result
      }
  }

  def redirect: Action[AnyContent] = Action.async(Future.successful(Redirect(controllers.routes.SchemesOverviewController.onPageLoad())))

}
