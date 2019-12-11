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
import controllers.actions.{AllowAccessForNonSuspendedUsersAction, AuthAction}
import forms.deregister.ConfirmStopBeingPsaFormProvider
import javax.inject.Inject
import models.MinimalPSA
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendBaseController, FrontendController}
import utils.annotations.PensionAdminCache
import views.html.deregister.confirmStopBeingPsa

import scala.concurrent.{ExecutionContext, Future}

class ConfirmStopBeingPsaController @Inject()(
                                               appConfig: FrontendAppConfig,
                                               auth: AuthAction,
                                               override val messagesApi: MessagesApi,
                                               formProvider: ConfirmStopBeingPsaFormProvider,
                                               minimalPsaConnector: MinimalPsaConnector,
                                               deregistrationConnector: DeregistrationConnector,
                                               enrolments: TaxEnrolmentsConnector,
                                               allowAccess: AllowAccessForNonSuspendedUsersAction,
                                               @PensionAdminCache dataCacheConnector: UserAnswersCacheConnector,
                                               val controllerComponents: MessagesControllerComponents,
                                               view: confirmStopBeingPsa
                                             )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = (auth andThen allowAccess).async {
    implicit request =>
      deregistrationConnector.canDeRegister(request.psaId.id).flatMap {
        case true =>
          minimalPsaConnector.getMinimalPsaDetails(request.psaId.id).map { minimalDetails =>
            getPsaName(minimalDetails) match {
              case Some(psaName) => Ok(view(form, psaName))
              case _ => Redirect(controllers.routes.SessionExpiredController.onPageLoad())
            }
          }
        case false =>
          Future.successful(Redirect(controllers.deregister.routes.CannotDeregisterController.onPageLoad()))
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
                  Future.successful(BadRequest(view(formWithErrors, psaName))),
                value => {
                  if (value) {
                    for {
                      _ <- deregistrationConnector.stopBeingPSA(psaId)
                      _ <- enrolments.deEnrol(userId, psaId, request.externalId)
                      _ <- dataCacheConnector.removeAll(request.externalId)
                    } yield {
                      Redirect(controllers.deregister.routes.SuccessfulDeregistrationController.onPageLoad())
                    }
                  } else {
                    Future.successful(Redirect(controllers.routes.SchemesOverviewController.onPageLoad()))
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
