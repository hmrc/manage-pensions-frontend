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
import connectors._
import connectors.scheme.{ListOfSchemesConnector, PensionSchemeVarianceLockConnector, SchemeDetailsConnector}
import controllers.actions._
import handlers.ErrorHandler
import identifiers.{SchemeNameId, SchemeSrnId, SchemeStatusId}
import javax.inject.Inject
import models._
import models.requests.AuthenticatedRequest
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import services.{HeaderCarrierFunctions, SchemeDetailsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.UserAnswers
import views.html.schemeDetails

import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsController @Inject()(appConfig: FrontendAppConfig,
                                        override val messagesApi: MessagesApi,
                                        schemeDetailsConnector: SchemeDetailsConnector,
                                        listSchemesConnector: ListOfSchemesConnector,
                                        schemeVarianceLockConnector: PensionSchemeVarianceLockConnector,
                                        authenticate: AuthAction,
                                        userAnswersCacheConnector: UserAnswersCacheConnector,
                                        errorHandler: ErrorHandler,
                                        val controllerComponents: MessagesControllerComponents,
                                        schemeDetailsService: SchemeDetailsService,
                                        view: schemeDetails
                                       )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = authenticate.async {
    implicit request =>
      withSchemeAndLock(srn).flatMap {
        case (userAnswers, lock) =>
          val admins = (userAnswers.json \ "psaDetails").as[Seq[PsaDetails]].map(_.id)

          if (admins.contains(request.psaId.id)) {

            val schemeName = userAnswers.get(SchemeNameId).getOrElse("")
            val schemeStatus = userAnswers.get(SchemeStatusId).getOrElse("")
            val isSchemeOpen = schemeStatus.equalsIgnoreCase("open")
            val updatedUa = userAnswers.set(SchemeSrnId)(srn.id).flatMap(_.set(SchemeNameId)(schemeName)).asOpt.getOrElse(userAnswers)
            val displayChangeLink = schemeDetailsService.displayChangeLink(isSchemeOpen, lock)
            for {
              aftHtml <- schemeDetailsService.retrieveAftHtml(userAnswers, srn.id)
              list <- listSchemesConnector.getListOfSchemes(request.psaId.id)
              _ <- userAnswersCacheConnector.upsert(request.externalId, updatedUa.json)
              lockingPsa <- schemeDetailsService.lockingPsa(lock, srn)
            } yield {
              Ok(view(
                schemeName,
                schemeDetailsService.pstr(srn.id, list),
                schemeDetailsService.openedDate(srn.id, list, isSchemeOpen),
                schemeDetailsService.administratorsVariations(request.psaId.id, userAnswers, schemeStatus),
                srn.id,
                isSchemeOpen,
                displayChangeLink,
                lockingPsa,
                aftHtml
              ))
            }
          } else {
            Future.successful(NotFound(errorHandler.notFoundTemplate))
          }
      }
  }


  private def withSchemeAndLock(srn: SchemeReferenceNumber)(implicit request: AuthenticatedRequest[AnyContent]): Future[(UserAnswers, Option[Lock])] = {
    for {
      _ <- userAnswersCacheConnector.removeAll(request.externalId)
      scheme <- schemeDetailsConnector.getSchemeDetails(request.psaId.id, "srn", srn)
      lock <- schemeVarianceLockConnector.isLockByPsaIdOrSchemeId(request.psaId.id, srn.id)
    } yield {
      (scheme, lock)
    }
  }
}
