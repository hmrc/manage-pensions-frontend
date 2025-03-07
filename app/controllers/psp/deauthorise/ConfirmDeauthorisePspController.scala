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

package controllers.psp.deauthorise

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaSchemeAuthAction}
import forms.psp.deauthorise.ConfirmDeauthPspFormProvider
import identifiers.psp.deauthorise
import identifiers.psp.deauthorise.{ConfirmDeauthorisePspId, PspDetailsId}
import identifiers.SchemeNameId
import models.{Index, NormalMode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.DeauthorisePSP
import utils.{Navigator, UserAnswers}
import views.html.psp.deauthorisation.confirmDeauthorisePsp

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmDeauthorisePspController @Inject()(
                                                 val appConfig: FrontendAppConfig,
                                                 val auth: AuthAction,
                                                 override val messagesApi: MessagesApi,
                                                 @DeauthorisePSP navigator: Navigator,
                                                 val formProvider: ConfirmDeauthPspFormProvider,
                                                 val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                 val getData: DataRetrievalAction,
                                                 val requireData: DataRequiredAction,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 view: confirmDeauthorisePsp,
                                                 psaSchemeAuthAction: PsaSchemeAuthAction
                                               )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(index: Index, srn: SchemeReferenceNumber): Action[AnyContent] =
                 (auth() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeNameId and deauthorise.PspDetailsId(index)).retrieve.map {
        case schemeName ~ pspDetails =>
          val preparedForm = request.userAnswers.get(deauthorise.ConfirmDeauthorisePspId(index)).fold(form)(form.fill)
          if (pspDetails.authorisingPSAID == request.psaIdOrException.id) {
            Future.successful(Ok(view(preparedForm, schemeName, srn, pspDetails.name, index)))
          } else {
            Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
          }
      }
  }

  def onSubmit(index: Index, srn: SchemeReferenceNumber): Action[AnyContent] =
              (auth() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeNameId and PspDetailsId(index)).retrieve.map {
        case schemeName ~ pspDetails =>
          form.bindFromRequest().fold(
            (formWithErrors: Form[Boolean]) =>
              Future.successful(BadRequest(view(formWithErrors, schemeName, srn, pspDetails.name, index))),
            value => {
              userAnswersCacheConnector.save(request.externalId, ConfirmDeauthorisePspId(index), value).map(
                cacheMap =>
                  Redirect(navigator.nextPage(deauthorise.ConfirmDeauthorisePspId(index), NormalMode, UserAnswers(cacheMap)))
              )
            }
          )
      }
  }
}
