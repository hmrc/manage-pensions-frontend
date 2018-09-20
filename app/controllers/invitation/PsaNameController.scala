/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.invitation

import javax.inject.Inject

import config.FrontendAppConfig
import connectors.DataCacheConnector
import controllers.actions._
import forms.invitation.PsaNameFormProvider
import identifiers.PsaNameId
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Action}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.Invitation
import utils.{Navigator, UserAnswers}
import views.html.invitation.psaName

import scala.concurrent.Future

class PsaNameController @Inject()(appConfig: FrontendAppConfig,
                                   override val messagesApi: MessagesApi,
                                   dataCacheConnector: DataCacheConnector,
                                   @Invitation navigator: Navigator,
                                   authenticate: AuthAction,
                                   getData: DataRetrievalAction,
                                   requireData: DataRequiredAction,
                                   formProvider: PsaNameFormProvider
                                 ) extends FrontendController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode) = (authenticate andThen getData).async {
    implicit request =>

      val value = request.userAnswers.flatMap(_.get(PsaNameId))
      val preparedForm = value.fold(form)(form.fill)

      Future.successful(Ok(psaName(appConfig, preparedForm, mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(psaName(appConfig, formWithErrors, mode))),

        (value) => {
          dataCacheConnector.save(request.externalId, PsaNameId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(PsaNameId, mode, UserAnswers(cacheMap)))
          )
        }
      )
  }
}
