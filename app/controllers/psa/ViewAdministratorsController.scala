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

import connectors.scheme.SchemeDetailsConnector
import controllers.actions._
import handlers.ErrorHandler
import identifiers.{SchemeNameId, SchemeStatusId}
import models._
import models.psa.PsaDetails
import models.requests.OptionalDataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SchemeDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers
import viewmodels.AssociatedPsa
import views.html.psa.viewAdministrators

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewAdministratorsController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              schemeDetailsConnector: SchemeDetailsConnector,
                                              authenticate: AuthAction,
                                              getData: DataRetrievalAction,
                                              errorHandler: ErrorHandler,
                                              val controllerComponents: MessagesControllerComponents,
                                              schemeDetailsService: SchemeDetailsService,
                                              view: viewAdministrators,
                                              psaSchemeAuthAction: PsaSchemeAuthAction
                                            )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(Some(srn))).async {
    implicit request =>
      getUserAnswers(srn).map { userAnswers =>

        val admins = (userAnswers.json \ "psaDetails").as[Seq[PsaDetails]].map(_.id)
        if (admins.contains(request.psaIdOrException.id)) {
          val schemeName = userAnswers.get(SchemeNameId).getOrElse("")
          val schemeStatus = userAnswers.get(SchemeStatusId).getOrElse("")
          val isSchemeOpen = schemeStatus.equalsIgnoreCase("open")
          val psaList: Option[Seq[AssociatedPsa]] =
            schemeDetailsService.administratorsVariations(request.psaIdOrException.id, userAnswers, schemeStatus)

          Ok(view(schemeName, psaList, srn.id, isSchemeOpen))

        } else {
          NotFound(errorHandler.notFoundTemplate)
        }
      }
  }

  private def getUserAnswers(srn: SchemeReferenceNumber)
                            (implicit request: OptionalDataRequest[AnyContent]): Future[UserAnswers] =
    request.userAnswers match {
      case Some(ua) if (ua.json \ "psaDetails").asOpt[Seq[PsaDetails]].nonEmpty => Future.successful(ua)
      case _ => schemeDetailsConnector.getSchemeDetails(
        psaId = request.psaIdOrException.id,
        idNumber = srn,
        schemeIdType = "srn"
      )
    }

}
