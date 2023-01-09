/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.invitations.psa

import com.google.inject.Inject
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.psa.AdviserEmailFormProvider
import identifiers.invitations.psa.{AdviserEmailId, AdviserNameId}
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AcceptInvitation
import utils.{Navigator, UserAnswers}
import views.html.invitations.psa.adviserEmailAddress

import scala.concurrent.{ExecutionContext, Future}

class AdviserEmailAddressController @Inject()(
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
      AdviserNameId.retrieve.map { adviserName =>
        val preparedForm = request.userAnswers.get(AdviserEmailId) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        Future.successful(Ok(view(preparedForm, mode, adviserName)))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      AdviserNameId.retrieve.map { adviserName =>
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
