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

package controllers.remove

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.psp.ConfirmRemovePspFormProvider
import identifiers.remove.{ConfirmRemovePspId, PspDetailsId}
import identifiers.{SchemeNameId, SchemeSrnId}
import javax.inject.Inject
import models.{Index, NormalMode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.RemovePSP
import utils.{Navigator, UserAnswers}
import viewmodels.Message
import views.html.psp.confirmRemovePsp

import scala.concurrent.{ExecutionContext, Future}

class ConfirmRemovePspController @Inject()(
                                            val appConfig: FrontendAppConfig,
                                            val auth: AuthAction,
                                            override val messagesApi: MessagesApi,
                                            @RemovePSP navigator: Navigator,
                                            val formProvider: ConfirmRemovePspFormProvider,
                                            val userAnswersCacheConnector: UserAnswersCacheConnector,
                                            val getData: DataRetrievalAction,
                                            val requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: confirmRemovePsp
                                          )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(index: Index): Action[AnyContent] = (auth() andThen getData andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId and PspDetailsId(index)).retrieve.right.map {
        case srn ~ schemeName ~ pspDetails =>
          val preparedForm = request.userAnswers.get(ConfirmRemovePspId(index)).fold(form)(form.fill)
          if (pspDetails.authorisingPSAID == request.psaIdOrException.id) {
            Future.successful(Ok(view(preparedForm, schemeName, srn, pspDetails.name, index)))
          } else {
            Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }
      }
  }

  def onSubmit(index: Index): Action[AnyContent] = (auth() andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and SchemeSrnId and PspDetailsId(index)).retrieve.right.map {
        case schemeName ~ srn ~ pspDetails =>
          form.bindFromRequest().fold(
            (formWithErrors: Form[Boolean]) =>
              Future.successful(BadRequest(view(formWithErrors, schemeName, srn, pspDetails.name, index))),
            value => {
              userAnswersCacheConnector.save(request.externalId, ConfirmRemovePspId(index), value).map(
                cacheMap =>
                  Redirect(navigator.nextPage(ConfirmRemovePspId(index), NormalMode, UserAnswers(cacheMap)))
              )
            }
          )
      }
  }
}
