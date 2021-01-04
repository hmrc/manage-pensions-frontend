/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import forms.invitations.AdviserEmailFormProvider
import identifiers.invitations.AdviserEmailId
import identifiers.invitations.AdviserNameId
import models.Mode
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.AcceptInvitation
import utils.Navigator
import utils.UserAnswers
import views.html.invitations.adviserEmailAddress

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AdviserEmailAddressController @Inject()(
                                               appConfig: FrontendAppConfig,
                                               override val messagesApi: MessagesApi,
                                               authenticate: AuthAction,
                                               @AcceptInvitation navigator: Navigator,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               formProvider: AdviserEmailFormProvider,
                                               dataCacheConnector: UserAnswersCacheConnector,
                                               val controllerComponents: MessagesControllerComponents,
                                               view: adviserEmailAddress
                                             )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {


  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      val form = formProvider()
      AdviserNameId.retrieve.right.map { adviserName =>
        val preparedForm = request.userAnswers.get(AdviserEmailId) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        Future.successful(Ok(view(preparedForm, mode, adviserName)))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      AdviserNameId.retrieve.right.map { adviserName =>
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) => {
            Future.successful(BadRequest(view(formWithErrors, mode, adviserName)))
          },
          value =>
            dataCacheConnector.save(request.externalId, AdviserEmailId, value).map {
              cacheMap =>
                Redirect(navigator.nextPage(AdviserEmailId, mode, UserAnswers(cacheMap)))
            }
        )
      }
  }

}
