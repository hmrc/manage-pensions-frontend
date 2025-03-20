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

package controllers.triagev2

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{DataRequiredAction, DataRetrievalAction, TriageAction, TriageToAuthAction}
import forms.triagev2.WhichServiceYouWantToViewFormProvider
import identifiers.triagev2.WhichServiceYouWantToViewId
import models.NormalMode
import models.triagev2.WhichServiceYouWantToView
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.TriageV2
import utils.{Enumerable, Navigator, UserAnswers}
import views.html.triagev2.whichServiceYouWantToView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhichServiceYouWantToViewController @Inject()(override val messagesApi: MessagesApi,
                                                    val appConfig: FrontendAppConfig,
                                                    triageAction: TriageAction,
                                                    triageToAuthAction: TriageToAuthAction,
                                                    @TriageV2 navigator: Navigator,
                                                    formProvider: WhichServiceYouWantToViewFormProvider,
                                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                                    getData: DataRetrievalAction,
                                                    requireData: DataRequiredAction,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    val view: whichServiceYouWantToView
                                           )(implicit val executionContext: ExecutionContext
                                           ) extends FrontendBaseController with I18nSupport with Enumerable.Implicits with Retrievals {

  private def form(role: String): Form[WhichServiceYouWantToView] = formProvider(role)

  def onPageLoad(role: String): Action[AnyContent] = (triageAction andThen triageToAuthAction andThen getData andThen requireData).async {
    implicit request =>
      val formInstance = form(role)
      val preparedForm = request.userAnswers.get(WhichServiceYouWantToViewId) match {
        case None => formInstance
        case Some(value) => formInstance.fill(value)
      }
      Future.successful(Ok(view(preparedForm, role)))
  }

  def onSubmit(role: String): Action[AnyContent] = (triageAction andThen triageToAuthAction andThen getData andThen requireData).async {
    implicit request =>
      form(role).bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, role))),
        value =>
          userAnswersCacheConnector.save(request.externalId, WhichServiceYouWantToViewId, value).map { cacheMap =>
            Redirect(navigator.nextPage(WhichServiceYouWantToViewId, NormalMode, UserAnswers(cacheMap)))
          }
      )
  }
}