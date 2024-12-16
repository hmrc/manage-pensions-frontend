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

package controllers.psp

import config.FrontendAppConfig
import connectors._
import connectors.admin.MinimalConnector
import connectors.scheme.{ListOfSchemesConnector, SchemeDetailsConnector}
import controllers.Retrievals
import controllers.actions._
import handlers.ErrorHandler
import identifiers.invitations.psp.PspClientReferenceId
import identifiers.psp.PSPNameId
import identifiers.{SchemeSrnId, SchemeStatusId}
import models.AuthEntity.PSP
import models._
import models.requests.AuthenticatedRequest
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import services.PsaSchemeDashboardService.{maxEndDateAsString, minStartDateAsString}
import services.{PspSchemeDashboardService, SchemeDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.SessionDataCache
import utils.{EventReportingHelper, UserAnswers}
import viewmodels.Message
import views.html.psp.pspSchemeDashboard

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PspSchemeDashboardController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              schemeDetailsConnector: SchemeDetailsConnector,
                                              authenticate: AuthAction,
                                              minimalConnector: MinimalConnector,
                                              errorHandler: ErrorHandler,
                                              listSchemesConnector: ListOfSchemesConnector,
                                              userAnswersCacheConnector: UserAnswersCacheConnector,
                                              @SessionDataCache sessionCacheConnector: UserAnswersCacheConnector,
                                              schemeDetailsService: SchemeDetailsService,
                                              val controllerComponents: MessagesControllerComponents,
                                              service: PspSchemeDashboardService,
                                              view: pspSchemeDashboard,
                                              appConfig: FrontendAppConfig,
                                              frontendConnector: FrontendConnector,
                                              pspSchemeAuthAction: PspSchemeAuthAction,
                                              getData: DataRetrievalAction,
                                              pensionSchemeReturnConnector: PensionSchemeReturnConnector
                                            )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  private val logger = Logger(classOf[PspSchemeDashboardController])

  //scalastyle:off method.length
  def onPageLoad(srn: String): Action[AnyContent] = (authenticate(PSP) andThen getData andThen pspSchemeAuthAction(srn)).async {
    implicit request =>
      withUserAnswers(srn) { userAnswers =>
        val pspDetails: AuthorisedPractitioner =
          (userAnswers.json \ "pspDetails").as[AuthorisedPractitioner]

        if (pspDetails.id == request.pspIdOrException.id) {
          val schemeStatus: String = userAnswers.get(SchemeStatusId).getOrElse("")

          val clientReference: Option[String] = userAnswers.get(PspClientReferenceId)

          val isSchemeOpen: Boolean =
            schemeStatus.equalsIgnoreCase("open")

          val schemeName = (userAnswers.json \ "schemeName").as[String]
          val pstr = (userAnswers.json \ "pstr").as[String]
          for {
            eqOverview <-  getPSROverview(srn, appConfig.showPsrLink, pstr)
            aftPspSchemeDashboardCards <- aftPspSchemeDashboardCards(schemeStatus, srn, pspDetails.authorisingPSAID, appConfig.hideAftTile)
            listOfSchemes <- listSchemesConnector.getListOfSchemesForPsp(request.pspIdOrException.id)
            _ <- userAnswersCacheConnector.upsert(request.externalId, userAnswers.json)
            erHtml <- getEventReportingHtml(srn, listOfSchemes, schemeName)
          } yield {
            listOfSchemes match {
              case Right(list) =>
                Ok(view(
                  schemeName = schemeName,
                  pstr = pstr,
                  isSchemeOpen = isSchemeOpen,
                  openDate = schemeDetailsService.openedDate(srn, list, isSchemeOpen),
                  schemeViewURL = appConfig.pspTaskListUrl.format(srn),
                  aftPspSchemeDashboardCards = aftPspSchemeDashboardCards,
                  evPspSchemeDashboardCard = erHtml,
                  cards = service.getTiles(
                    erHtml = erHtml,
                    srn = srn,
                    pstr = pstr,
                    openDate = schemeDetailsService.openedDate(srn, list, isSchemeOpen),
                    loggedInPsp = pspDetails,
                    clientReference = clientReference,
                    eqOverview
                  ),
                  returnLink = Some(Link(
                    id = "return-search-schemes",
                    url = controllers.psp.routes.ListSchemesController.onPageLoad.url,
                    linkText = Message("messages__psaSchemeDash__return_link").resolve
                  ))
                ))
              case _ =>
                NotFound(errorHandler.notFoundTemplate)
            }
          }
        } else {
          logger.debug("PSP tried to access an unauthorised scheme")
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
        }
      }
  }

  private def getPSROverview(srn: SchemeReferenceNumber, showPsrLink: Boolean,
                          pstr: String)(implicit hc: HeaderCarrier) = {
    if (showPsrLink && pstr.nonEmpty) {
      pensionSchemeReturnConnector.getOverview(srn, pstr,minStartDateAsString, maxEndDateAsString)
        .recoverWith {
          case e =>
            logger.error("Issue with PSR request", e)
            Future.successful(Seq.empty)
        }
    } else {
      Future.successful(Seq.empty)
    }
  }
  private def getEventReportingHtml(srn: String, list: Either[_, ListOfSchemes], schemeName: String)(implicit authenticatedRequest: AuthenticatedRequest[_]) = {
    list match {
      case Left(_) =>
        Future.successful(Html(""))
      case Right(listOfSchemes) =>
        val eventReportingData = EventReportingHelper.eventReportingData(
          srn,
          listOfSchemes,
          pstr => EventReporting(
            pstr = pstr,
            schemeName = schemeName,
            returnUrl = appConfig.pspSchemeDashboardUrl.format(srn),
            psaId = None,
            pspId = Some(authenticatedRequest.pspIdOrException.id),
            srn = srn
          ))
        eventReportingData.map { data =>
          EventReportingHelper.storeData(sessionCacheConnector, data).flatMap { _ =>
            frontendConnector.retrieveEventReportingPartial
          }
        }.getOrElse(Future.successful(Html("")))
    }

  }

  private def aftPspSchemeDashboardCards(schemeStatus: String, srn: String, authorisingPsaId: String, hideTile: Boolean)
                                        (implicit request: AuthenticatedRequest[AnyContent]): Future[Html] =
    if (
      (schemeStatus.equalsIgnoreCase("open") ||
        schemeStatus.equalsIgnoreCase("wound-up") ||
        schemeStatus.equalsIgnoreCase("deregistered")) && !hideTile
    ) {
      schemeDetailsService.retrievePspSchemeDashboardCards(srn, request.pspIdOrException.id, authorisingPsaId)
    } else {
      Future.successful(Html(""))
    }

  private def withUserAnswers(srn: String)(block: UserAnswers => Future[Result])
                             (implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val requiredDetails = for {
      _ <- userAnswersCacheConnector.removeAll(request.externalId)
      userAnswers <- schemeDetailsConnector.getPspSchemeDetails(request.pspIdOrException.id, srn)
      minPspDetails <- minimalConnector.getMinimalPspDetails()
    } yield {
      (userAnswers, minPspDetails)
    }

    requiredDetails.flatMap { case (userAnswers, minPspDetails) =>
      if (minPspDetails.deceasedFlag) {
        Future.successful(Redirect(controllers.routes.ContactHMRCController.onPageLoad()))
      } else if (minPspDetails.rlsFlag) {
        Future.successful(Redirect(appConfig.pspUpdateContactDetailsUrl))
      } else {
        val ua =
          userAnswers
            .set(SchemeSrnId)(srn)
            .flatMap(_.set(PSPNameId)(minPspDetails.name))
            .asOpt
            .getOrElse(userAnswers)

        block(ua)
      }
    }
  }
}
