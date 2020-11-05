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
import connectors.scheme.SchemeDetailsConnector
import controllers.actions._
import identifiers.{PSPNameId, SchemeSrnId}
import javax.inject.Inject
import models.AuthEntity.PSP
import models._
import models.requests.AuthenticatedRequest
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.UserAnswers
import views.html.pspSchemeDashboard

import scala.concurrent.{ExecutionContext, Future}

class PspSchemeDashboardController @Inject()(override val messagesApi: MessagesApi,
                                             schemeDetailsConnector: SchemeDetailsConnector,
                                             authenticate: AuthAction,
                                             minimalConnector: MinimalConnector,
                                             userAnswersCacheConnector: UserAnswersCacheConnector,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: pspSchemeDashboard
                                            )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: String): Action[AnyContent] = authenticate(PSP).async {
    implicit request =>
      getUserAnswers(srn).flatMap { userAnswers =>
        val pspList = (userAnswers.json \ "pspDetails").as[Seq[AuthorisedPractitioner]].map(_.id)
        if (pspList.contains(request.pspIdOrException.id)) {
          userAnswersCacheConnector.upsert(request.externalId, userAnswers.json).map { _ =>
            Ok(view())
          }
        } else {
          Logger.debug("PSP tried to access an unauthorised scheme")
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
        }
      }
  }

  private def getUserAnswers(srn: String)(implicit request: AuthenticatedRequest[AnyContent]): Future[UserAnswers] =
    for {
      _ <- userAnswersCacheConnector.removeAll(request.externalId)
      userAnswers <- schemeDetailsConnector.getSchemeDetails(request.pspIdOrException.id, "srn", srn)
      minPspDetails <- minimalConnector.getMinimalPspDetails(request.pspIdOrException.id)
    } yield {
      userAnswers.set(SchemeSrnId)(srn)
        .flatMap(_.set(PSPNameId)(minPspDetails.name)).asOpt
        .getOrElse(userAnswers)
    }
}
