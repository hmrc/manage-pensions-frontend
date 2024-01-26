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

package controllers.psa

import config.FrontendAppConfig
import connectors._
import connectors.admin.{DelimitedAdminException, MinimalConnector}
import connectors.scheme.{ListOfSchemesConnector, PensionSchemeVarianceLockConnector, SchemeDetailsConnector}
import controllers.actions._
import identifiers.{SchemeNameId, SchemeSrnId, SchemeStatusId}
import models._
import models.requests.AuthenticatedRequest
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import services.PsaSchemeDashboardService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{EventReportingHelper, UserAnswers}
import utils.annotations.SessionDataCache
import views.html.psa.psaSchemeDashboard

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsaSchemeDashboardController @Inject()(override val messagesApi: MessagesApi,
                                             schemeDetailsConnector: SchemeDetailsConnector,
                                             listSchemesConnector: ListOfSchemesConnector,
                                             schemeVarianceLockConnector: PensionSchemeVarianceLockConnector,
                                             authenticate: AuthAction,
                                             userAnswersCacheConnector: UserAnswersCacheConnector,
                                             @SessionDataCache sessionDataCacheConnector: UserAnswersCacheConnector,
                                             val controllerComponents: MessagesControllerComponents,
                                             psaSchemeDashboardService: PsaSchemeDashboardService,
                                             view: psaSchemeDashboard,
                                             frontendConnector: FrontendConnector,
                                             minimalPsaConnector: MinimalConnector,
                                             val appConfig: FrontendAppConfig,
                                             psaSchemeAction: PsaSchemeAction,
                                             getData: DataRetrievalAction
                                            )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val logger = Logger(classOf[PsaSchemeDashboardController])

  private val tileRecover: Future[Html] => Future[Html] = f => f.recover {
    ex =>
      logger.warn("Retrieval of tile failed", ex)
      Html("")
  }

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen
    getData andThen psaSchemeAction(Some(srn))).async {
    implicit request =>

      minimalPsaConnector.getMinimalPsaDetails(request.psaIdOrException.id).flatMap { minimalDetails =>
        (minimalDetails.deceasedFlag, minimalDetails.rlsFlag) match {
          case (true, _) => Future.successful(Redirect(controllers.routes.ContactHMRCController.onPageLoad()))
          case (_, true) => Future.successful(Redirect(appConfig.psaUpdateContactDetailsUrl))
          case _ =>
            getSchemeAndLock(srn).flatMap { case (userAnswers, lock, listOfSchemes) =>
              val schemeName = userAnswers.get(SchemeNameId).getOrElse("")
              val schemeStatus = userAnswers.get(SchemeStatusId).getOrElse("")
              val updatedUa = userAnswers.set(SchemeSrnId)(srn.id)
                .flatMap(_.set(SchemeNameId)(schemeName))
                .asOpt.getOrElse(userAnswers)

              val eventReportingData: Option[EventReportingHelper.EventReportingData] = EventReportingHelper.eventReportingData(
                srn,
                listOfSchemes,
                pstr => EventReporting(
                  pstr = pstr,
                  schemeName = schemeName,
                  returnUrl = appConfig.psaSchemeDashboardUrl.format(srn.id),
                  psaId = Some(request.psaIdOrException.id),
                  pspId = None,
                  srn = srn.id
                )
              )
              for {
                aftHtml <- tileRecover(retrieveAftTilesHtml(srn, schemeStatus))
                finInfoHtml <- tileRecover(retrieveFinInfoTilesHtml(srn, schemeStatus))
                _ <- userAnswersCacheConnector.upsert(request.externalId, updatedUa.json)
                _ <- eventReportingData.map { data =>
                  EventReportingHelper.storeData(sessionDataCacheConnector, data)
                }.getOrElse(Future.successful(Json.obj()))
                erHtml <- eventReportingData.map(_ => frontendConnector.retrieveEventReportingPartial)
                  .getOrElse(Future.successful(Html("")))
                cards <- psaSchemeDashboardService.cards(srn, lock, listOfSchemes, userAnswers)
              } yield {
                Ok(view(schemeName, aftHtml, finInfoHtml, erHtml, cards))
              }
            }
        }
      } recoverWith {
        case _: DelimitedAdminException =>
          Future.successful(Redirect(controllers.routes.DelimitedAdministratorController.onPageLoad))
      }
  }

  private def retrieveAftTilesHtml(
                                    srn: String,
                                    schemeStatus: String
                                  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Html] = {
    if (
      schemeStatus.equalsIgnoreCase("open") ||
        schemeStatus.equalsIgnoreCase("wound-up") ||
        schemeStatus.equalsIgnoreCase("deregistered")
    ) {
      frontendConnector.retrieveAftPartial(srn)
    } else {
      Future.successful(Html(""))
    }
  }
  private def retrieveFinInfoTilesHtml(
                                    srn: String,
                                    schemeStatus: String
                                  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Html] = {
    if (
      schemeStatus.equalsIgnoreCase("open") ||
        schemeStatus.equalsIgnoreCase("wound-up") ||
        schemeStatus.equalsIgnoreCase("deregistered")
    ) {
      frontendConnector.retrieveFinInfoPartial(srn)
    } else {
      Future.successful(Html(""))
    }
  }

  private def getSchemeAndLock(srn: SchemeReferenceNumber)
                              (implicit request: AuthenticatedRequest[AnyContent]): Future[(UserAnswers, Option[Lock], ListOfSchemes)] = {
    for {
      _ <- userAnswersCacheConnector.removeAll(request.externalId)
      scheme <- schemeDetailsConnector.getSchemeDetails(request.psaIdOrException.id, srn, "srn")
      lock <- schemeVarianceLockConnector.isLockByPsaIdOrSchemeId(request.psaIdOrException.id, srn.id)
      listOfSchemes <- listSchemesConnector.getListOfSchemes(request.psaIdOrException.id)
    } yield {
      listOfSchemes match {
        case Right(list) => (scheme, lock, list)
        case _ => throw ListOfSchemesRetrievalException
      }
    }
  }
}

case object ListOfSchemesRetrievalException extends Exception
