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

package controllers.deregister

import config.FrontendAppConfig
import connectors.{DeregistrationConnector, MinimalPsaConnector, TaxEnrolmentsConnector, UserAnswersCacheConnector}
import controllers.actions.{AllowAccessForNonSuspendedUsersActionProvider, AuthAction}
import forms.deregister.ConfirmStopBeingPsaFormProvider
import javax.inject.Inject
import models.MinimalPSA
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.PensionAdminCache
import views.html.deregister.confirmStopBeingPsa

import scala.concurrent.{ExecutionContext, Future}

class ConfirmStopBeingPsaController @Inject()(
                                               appConfig: FrontendAppConfig,
                                               auth: AuthAction,
                                               val messagesApi: MessagesApi,
                                               formProvider: ConfirmStopBeingPsaFormProvider,
                                               minimalPsaConnector: MinimalPsaConnector,
                                               deregistration: DeregistrationConnector,
                                               enrolments: TaxEnrolmentsConnector,
                                               allowAccess: AllowAccessForNonSuspendedUsersActionProvider,
                                               @PensionAdminCache dataCacheConnector: UserAnswersCacheConnector
                                             )(implicit val ec: ExecutionContext) extends FrontendController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = (auth andThen allowAccess()).async {
    implicit request =>
      minimalPsaConnector.getMinimalPsaDetails(request.psaId.id).map { minimalDetails =>
        getPsaName(minimalDetails) match {
          case Some(psaName) => Ok(confirmStopBeingPsa(appConfig, form, psaName))
          case _ => Redirect(controllers.routes.SessionExpiredController.onPageLoad())
        }
      }
  }

  def onSubmit: Action[AnyContent] = auth.async {
    implicit request =>
      val psaId = request.psaId.id
      val userId = request.userId
      minimalPsaConnector.getMinimalPsaDetails(psaId).flatMap {
        minimalDetails =>
          getPsaName(minimalDetails) match {
            case Some(psaName) =>
              form.bindFromRequest().fold(
                (formWithErrors: Form[Boolean]) =>
                  Future.successful(BadRequest(confirmStopBeingPsa(appConfig, formWithErrors, psaName))),
                value => {
                  if (value) {
                    for {
                      _ <- deregistration.stopBeingPSA(psaId)
                      _ <- enrolments.deEnrol(userId, psaId, request.externalId)
                      _ <- dataCacheConnector.removeAll(request.externalId)
                    } yield {
                      Redirect(controllers.deregister.routes.SuccessfulDeregistrationController.onPageLoad())
                    }
                  } else {
                    Future.successful(Redirect(appConfig.registeredPsaDetailsUrl))
                  }
                }
              )
            case _ =>
              Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }
      }
  }

  private def getPsaName(minimalDetails: MinimalPSA): Option[String] = {
    (minimalDetails.individualDetails, minimalDetails.organisationName) match {
      case (Some(individual), None) => Some(individual.fullName)
      case (None, Some(org)) => Some(s"$org")
      case _ => None
    }
  }

}
