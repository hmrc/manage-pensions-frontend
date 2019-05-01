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

package controllers.invitations

import com.google.inject.{Inject, Singleton}
import config.FeatureSwitchManagementService
import connectors.{MinimalPsaConnector, SchemeDetailsConnector, UserAnswersCacheConnector}
import controllers.actions.AuthAction
import identifiers.invitations.PSTRId
import identifiers.{MinimalSchemeDetailId, SchemeNameId}
import models.requests.AuthenticatedRequest
import models.{MinimalSchemeDetail, SchemeReferenceNumber}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Toggles

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InviteController @Inject()(authenticate: AuthAction,
                                 schemeDetailsConnector: SchemeDetailsConnector,
                                 userAnswersCacheConnector: UserAnswersCacheConnector,
                                 featureSwitchManagementService: FeatureSwitchManagementService,
                                 minimalPsaConnector: MinimalPsaConnector)(implicit val ec: ExecutionContext) extends FrontendController {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = authenticate.async {
    implicit request =>
      minimalPsaConnector.getMinimalPsaDetails(request.psaId.id).flatMap { minimalPsaDetails =>
        if (minimalPsaDetails.isPsaSuspended) {
          Future.successful(Redirect(controllers.invitations.routes.YouCannotSendAnInviteController.onPageLoad()))
        } else {
          getSchemeDetails(srn) flatMap {
              case Some(x) =>
                val minimalSchemeDetail = MinimalSchemeDetail(srn, x.pstr, x.name)
                userAnswersCacheConnector.save(request.externalId, MinimalSchemeDetailId, minimalSchemeDetail).map { _ =>
                  Redirect(controllers.invitations.routes.WhatYouWillNeedController.onPageLoad())
                }
              case None => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
            }
        }
      }
  }

  private def getSchemeDetails(srn: SchemeReferenceNumber)(implicit request: AuthenticatedRequest[_]): Future[Option[SchemeDetails]] =
    if (featureSwitchManagementService.get(Toggles.isVariationsEnabled)) {

      schemeDetailsConnector.getSchemeDetailsVariations(request.psaId.id, "srn", srn).map { scheme =>
        scheme.get(SchemeNameId).flatMap { name =>
          Some(SchemeDetails(name, scheme.get(PSTRId)))
        }

      }
    } else {
      schemeDetailsConnector.getSchemeDetails(request.psaId.id, "srn", srn).map { scheme =>
        Some(SchemeDetails(scheme.schemeDetails.name, scheme.schemeDetails.pstr))
      }
    }

  case class SchemeDetails(name: String, pstr: Option[String])

}
