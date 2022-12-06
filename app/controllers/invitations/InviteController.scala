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

package controllers.invitations

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import connectors.admin.{DelimitedAdminException, MinimalConnector}
import connectors.scheme.SchemeDetailsConnector
import controllers.actions.AuthAction
import identifiers.{MinimalSchemeDetailId, SchemeNameId}
import identifiers.invitations.PSTRId
import models.{MinimalSchemeDetail, SchemeReferenceNumber}
import models.requests.AuthenticatedRequest
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InviteController @Inject()(
                                  authenticate: AuthAction,
                                  schemeDetailsConnector: SchemeDetailsConnector,
                                  userAnswersCacheConnector: UserAnswersCacheConnector,
                                  minimalPsaConnector: MinimalConnector,
                                  val controllerComponents: MessagesControllerComponents,
                                  appConfig: FrontendAppConfig
                                )(implicit val ec: ExecutionContext) extends FrontendBaseController {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = authenticate().async {
    implicit request =>

      minimalPsaConnector.getMinimalPsaDetails(request.psaIdOrException.id).flatMap { minimalPsaDetails =>
        if (minimalPsaDetails.isPsaSuspended) {
          Future.successful(Redirect(controllers.invitations.routes.YouCannotSendAnInviteController.onPageLoad()))
        } else if (minimalPsaDetails.deceasedFlag) {
          Future.successful(Redirect(controllers.routes.ContactHMRCController.onPageLoad()))
        } else if (minimalPsaDetails.rlsFlag) {
          Future.successful(Redirect(appConfig.psaUpdateContactDetailsUrl))
        } else {
          getSchemeDetails(srn) flatMap {
            case Some(schemeDetails) =>
              val minimalSchemeDetail = MinimalSchemeDetail(srn, schemeDetails.pstr, schemeDetails.name)
              userAnswersCacheConnector.save(request.externalId, MinimalSchemeDetailId, minimalSchemeDetail).map { _ =>
                Redirect(controllers.invitations.psa.routes.WhatYouWillNeedController.onPageLoad())
              }
            case None => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
          }
        }
      } recoverWith {
        case _: DelimitedAdminException =>
          Future.successful(Redirect(controllers.routes.DelimitedAdministratorController.onPageLoad))
      }
  }

  private def getSchemeDetails(srn: SchemeReferenceNumber)(implicit request: AuthenticatedRequest[_]): Future[Option[SchemeDetails]] =
    schemeDetailsConnector.getSchemeDetails(
      psaId = request.psaIdOrException.id,
      idNumber = srn,
      schemeIdType = "srn"
    ) map { scheme =>
      scheme.get(SchemeNameId).flatMap { name =>
        Some(SchemeDetails(name, scheme.get(PSTRId)))
      }

    }

  case class SchemeDetails(name: String, pstr: Option[String])

}
