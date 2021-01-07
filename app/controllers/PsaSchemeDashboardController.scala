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

import connectors._
import connectors.scheme.{ListOfSchemesConnector, PensionSchemeVarianceLockConnector, SchemeDetailsConnector}
import controllers.actions._
import handlers.ErrorHandler
import identifiers.{SchemeNameId, SchemeSrnId, SchemeStatusId}
import javax.inject.Inject
import models.FeatureToggle.Enabled
import models.FeatureToggleName.PSPAuthorisation
import models._
import models.requests.AuthenticatedRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsArray
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import services.{FeatureToggleService, SchemeDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers
import viewmodels.Message
import views.html.schemeDetails

import scala.concurrent.{ExecutionContext, Future}

class PsaSchemeDashboardController @Inject()(override val messagesApi: MessagesApi,
                                             schemeDetailsConnector: SchemeDetailsConnector,
                                             listSchemesConnector: ListOfSchemesConnector,
                                             schemeVarianceLockConnector: PensionSchemeVarianceLockConnector,
                                             authenticate: AuthAction,
                                             userAnswersCacheConnector: UserAnswersCacheConnector,
                                             errorHandler: ErrorHandler,
                                             val controllerComponents: MessagesControllerComponents,
                                             schemeDetailsService: SchemeDetailsService,
                                             view: schemeDetails,
                                             featureToggleService:FeatureToggleService
                                       )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = authenticate().async {
    implicit request =>
      withSchemeAndLock(srn).flatMap { case (userAnswers, lock) =>
          val admins = (userAnswers.json \ "psaDetails").as[Seq[PsaDetails]].map(_.id)
          val anyPSPs = (userAnswers.json \ "pspDetails").asOpt[JsArray].exists(_.value.nonEmpty)
          if (admins.contains(request.psaIdOrException.id)) {
            val schemeName = userAnswers.get(SchemeNameId).getOrElse("")
            val schemeStatus = userAnswers.get(SchemeStatusId).getOrElse("")
            val isSchemeOpen = schemeStatus.equalsIgnoreCase("open")
            val updatedUa = userAnswers.set(SchemeSrnId)(srn.id).flatMap(_.set(SchemeNameId)(schemeName)).asOpt.getOrElse(userAnswers)
            val displayChangeLink = schemeDetailsService.displayChangeLink(isSchemeOpen, lock)
            for {
              aftHtml <- schemeDetailsService.retrieveAftHtml(userAnswers, srn.id)
              paymentsAndChargesHtml <- retrievePaymentsAndChargesHtml(srn.id, isSchemeOpen)
              listOfSchemes <- listSchemesConnector.getListOfSchemes(request.psaIdOrException.id)
              _ <- userAnswersCacheConnector.upsert(request.externalId, updatedUa.json)
              lockingPsa <- schemeDetailsService.lockingPsa(lock, srn)
              pspLinks <- getPspLinks(anyPSPs)
            } yield {
              listOfSchemes match {
                case Right(list) =>
                  Ok(view(
                    schemeName,
                    schemeDetailsService.pstr(srn.id, list),
                    schemeDetailsService.openedDate(srn.id, list, isSchemeOpen),
                    srn.id,
                    isSchemeOpen,
                    displayChangeLink,
                    lockingPsa,
                    aftHtml,
                    paymentsAndChargesHtml,
                    pspLinks
                  ))
                case _ => NotFound(errorHandler.notFoundTemplate)
              }
            }
          } else {
            Future.successful(NotFound(errorHandler.notFoundTemplate))
          }
      }
  }

  private def retrievePaymentsAndChargesHtml(
                                              srn: String,
                                              isSchemeOpen: Boolean
                                            )(implicit request: AuthenticatedRequest[AnyContent]): Future[Html] = {
    if (isSchemeOpen) {
      schemeDetailsService.retrievePaymentsAndChargesHtml(srn)
    } else {
      Future.successful(Html(""))
    }
  }

  private def getPspLinks(anyPSPs:Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext):Future[Seq[Link]] = {
    featureToggleService.get(PSPAuthorisation).map {
      case Enabled(PSPAuthorisation) =>
      val viewPspLink = if (anyPSPs) {
        Seq(Link(
          id = "view-practitioners",
          url = controllers.psp.routes.ViewPractitionersController.onPageLoad().url,
          linkText = Message("messages__pspViewOrDeauthorise__link")
        ))
      } else {
        Nil
      }
      Seq(
        Link("authorise", controllers.invitations.psp.routes.WhatYouWillNeedController.onPageLoad().url, Message("messages__pspAuthorise__link"))
      ) ++ viewPspLink
      case _ =>
        Nil
    }
 }

  private def withSchemeAndLock(srn: SchemeReferenceNumber)
                               (implicit request: AuthenticatedRequest[AnyContent]): Future[(UserAnswers, Option[Lock])] = {
    for {
      _ <- userAnswersCacheConnector.removeAll(request.externalId)
      scheme <- schemeDetailsConnector.getSchemeDetails(
        psaId = request.psaIdOrException.id,
        idNumber = srn,
        schemeIdType = "srn"
      )
      lock <- schemeVarianceLockConnector.isLockByPsaIdOrSchemeId(request.psaIdOrException.id, srn.id)
    } yield {
      (scheme, lock)
    }
  }
}
