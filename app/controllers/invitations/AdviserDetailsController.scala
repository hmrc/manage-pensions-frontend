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

package controllers.invitations

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.DataCacheConnector
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.AdviserDetailsFormProvider
import identifiers.invitations.AdviserNameId
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.AcceptInvitation
import utils.{Navigator, UserAnswers}
import views.html.invitations.adviserDetails

import scala.concurrent.Future

class AdviserDetailsController @Inject()(
                                          appConfig: FrontendAppConfig,
                                          override val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          @AcceptInvitation navigator: Navigator,
                                          getData: DataRetrievalAction,
                                          requiredData: DataRequiredAction,
                                          formProvider: AdviserDetailsFormProvider,
                                          dataCacheConnector: DataCacheConnector
                                        ) extends FrontendController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requiredData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(AdviserNameId) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Future.successful(Ok(adviserDetails(appConfig, preparedForm, mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requiredData).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(adviserDetails(appConfig, formWithErrors, mode))),
        value => {
          dataCacheConnector.save(request.externalId, AdviserNameId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(AdviserNameId, mode, UserAnswers(cacheMap)))
          )
        }
      )
  }

}
