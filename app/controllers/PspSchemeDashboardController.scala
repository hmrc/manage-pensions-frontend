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

import connectors._
import connectors.admin.MinimalConnector
import connectors.scheme.{ListOfSchemesConnector, SchemeDetailsConnector}
import controllers.actions._
import handlers.ErrorHandler
import identifiers.invitations.psp.PspClientReferenceId
import identifiers.{PSPNameId, SchemeSrnId, SchemeStatusId}
import javax.inject.Inject
import models.AuthEntity.PSP
import models._
import models.invitations.psp.ClientReference
import models.requests.AuthenticatedRequest
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PspSchemeDashboardService, SchemeDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.UserAnswers
import views.html.pspSchemeDashboard

import scala.concurrent.{ExecutionContext, Future}

class PspSchemeDashboardController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              schemeDetailsConnector: SchemeDetailsConnector,
                                              authenticate: AuthAction,
                                              minimalConnector: MinimalConnector,
                                              errorHandler: ErrorHandler,
                                              listSchemesConnector: ListOfSchemesConnector,
                                              userAnswersCacheConnector: UserAnswersCacheConnector,
                                              schemeDetailsService: SchemeDetailsService,
                                              val controllerComponents: MessagesControllerComponents,
                                              service: PspSchemeDashboardService,
                                              view: pspSchemeDashboard
                                            )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def onPageLoad(srn: String): Action[AnyContent] = authenticate(PSP).async {
    implicit request =>
      getUserAnswers(srn).flatMap {
        userAnswers =>
          val pspList = (userAnswers.json \ "pspDetails").as[Seq[AuthorisedPractitioner]]
          val pspIdList: Seq[String] = pspList.map(_.id)

          if (pspIdList.contains(request.pspIdOrException.id)) {
            val schemeStatus: String = userAnswers.get(SchemeStatusId).getOrElse("")
            val clientReference: Option[String] = userAnswers.get(PspClientReferenceId).flatMap {
              case ClientReference.HaveClientReference(reference) => Some(reference)
              case ClientReference.NoClientReference => None
            }
            val isSchemeOpen: Boolean = schemeStatus.equalsIgnoreCase("open")
            val loggedInPsp: AuthorisedPractitioner =
              pspList
                .find(_.id == request.pspIdOrException.id)
                .getOrElse(throw new Exception("No logged in PSP found"))

            for {
              aftCards <- schemeDetailsService.retrievePspDashboardAftCards(srn, request.pspIdOrException.id)
              listOfSchemes <- listSchemesConnector.getListOfSchemesForPsp(request.pspIdOrException.id)
              _ <- userAnswersCacheConnector.upsert(request.externalId, userAnswers.json)
            } yield {
              listOfSchemes match {
                case Right(list) =>
                  Ok(view(
                    schemeName = (userAnswers.json \ "schemeName").as[String],
                    aftCards = Seq(aftCards),
                    cards = service.getTiles(
                      srn = srn,
                      pstr = (userAnswers.json \ "pstr").as[String],
                      openDate = schemeDetailsService.openedDate(srn, list, isSchemeOpen),
                      loggedInPsp = loggedInPsp,
                      clientReference = clientReference
                    )
                  ))
                case _ =>
                  NotFound(errorHandler.notFoundTemplate)
              }
            }
          } else {
            Logger.debug("PSP tried to access an unauthorised scheme")
            Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }

      }
  }


  private def getUserAnswers(srn: String)
                            (implicit request: AuthenticatedRequest[AnyContent]): Future[UserAnswers] =
    for {
      _ <- userAnswersCacheConnector.removeAll(request.externalId)
      userAnswers <- schemeDetailsConnector.getSchemeDetails(
        psaId = request.pspIdOrException.id,
        idNumber = srn,
        schemeIdType = "srn"
      )
      minPspDetails <- minimalConnector.getMinimalPspDetails(request.pspIdOrException.id)
    } yield {
      userAnswers.set(SchemeSrnId)(srn)
        .flatMap(_.set(PSPNameId)(minPspDetails.name)).asOpt
        .getOrElse(userAnswers)
    }
}
