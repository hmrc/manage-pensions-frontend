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

package controllers.remove

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.remove.RemovalDateFormProvider
import identifiers.SchemeSrnId
import identifiers.invitations.{PSANameId, SchemeNameId}
import identifiers.remove.RemovalDateId
import javax.inject.Inject
import models.NormalMode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.Invitation
import utils.{Navigator, UserAnswers}
import views.html.remove.removalDate

import scala.concurrent.Future

class RemovalDateController @Inject()(appConfig: FrontendAppConfig,
                                              override val messagesApi: MessagesApi,
                                              dataCacheConnector: UserAnswersCacheConnector,
                                              @Invitation navigator: Navigator,
                                              authenticate: AuthAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              formProvider: RemovalDateFormProvider) extends FrontendController with I18nSupport with Retrievals {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSANameId and SchemeSrnId).retrieve.right.map {
        case schemeName ~ psaName ~ srn =>
          Future.successful(Ok(removalDate(appConfig, form, psaName, schemeName, srn)))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          (SchemeNameId and PSANameId and SchemeSrnId).retrieve.right.map {
            case schemeName ~ psaName ~ srn =>
              Future.successful(BadRequest(removalDate(appConfig, formWithErrors, psaName, schemeName, srn)))
          },
        value =>
          dataCacheConnector.save(request.externalId, RemovalDateId, value).map(cacheMap =>
            Redirect(navigator.nextPage(RemovalDateId, NormalMode, UserAnswers(cacheMap))))
      )
  }
}
