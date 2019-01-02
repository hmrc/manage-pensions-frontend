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

package controllers.invitations

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.AdviserEmailFormProvider
import identifiers.invitations.{AdviserEmailId, AdviserNameId}
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{Navigator, UserAnswers}
import utils.annotations.AcceptInvitation
import views.html.invitations.adviserEmailAddress

import scala.concurrent.{ExecutionContext, Future}

class AdviserEmailAddressController @Inject()(
                                               appConfig: FrontendAppConfig,
                                               override val messagesApi: MessagesApi,
                                               authenticate: AuthAction,
                                               @AcceptInvitation navigator: Navigator,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               formProvider: AdviserEmailFormProvider,
                                               dataCacheConnector: UserAnswersCacheConnector
                                             )(implicit val ec: ExecutionContext) extends FrontendController with I18nSupport with Retrievals {


  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      val form = formProvider()
      AdviserNameId.retrieve.right.map { adviserName =>
        val preparedForm = request.userAnswers.get(AdviserEmailId) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        Future.successful(Ok(adviserEmailAddress(appConfig, preparedForm, mode, adviserName)))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      AdviserNameId.retrieve.right.map { adviserName =>
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) => {
            Future.successful(BadRequest(adviserEmailAddress(appConfig, formWithErrors, mode, adviserName)))
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
