/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.psa.AdviserDetailsFormProvider
import identifiers.invitations.psa.AdviserNameId
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AcceptInvitation
import utils.{Navigator, UserAnswers}
import views.html.invitations.psa.adviserDetails

import scala.concurrent.{ExecutionContext, Future}

class AdviserDetailsController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          @AcceptInvitation navigator: Navigator,
                                          getData: DataRetrievalAction,
                                          requiredData: DataRequiredAction,
                                          formProvider: AdviserDetailsFormProvider,
                                          dataCacheConnector: UserAnswersCacheConnector,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: adviserDetails
                                        )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requiredData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(AdviserNameId) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Future.successful(Ok(view(preparedForm, mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requiredData).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, mode))),
        value => {
          dataCacheConnector.save(request.externalId, AdviserNameId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(AdviserNameId, mode, UserAnswers(cacheMap)))
          )
        }
      )
  }

}
