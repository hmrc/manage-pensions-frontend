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

package controllers.invitations.psa

import connectors.UserAnswersCacheConnector
import controllers.actions._
import forms.invitations.psa.PsaNameFormProvider
import identifiers.invitations.InviteeNameId
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.Invitations
import utils.{Navigator, UserAnswers}
import views.html.invitations.psa.psaName

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsaNameController @Inject()(
                                   override val messagesApi: MessagesApi,
                                   dataCacheConnector: UserAnswersCacheConnector,
                                   @Invitations navigator: Navigator,
                                   authenticate: AuthAction,
                                   getData: DataRetrievalAction,
                                   formProvider: PsaNameFormProvider,
                                   val controllerComponents: MessagesControllerComponents,
                                   view: psaName
                                 )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate() andThen getData).async {
      implicit request =>

        val value = request.userAnswers.flatMap(_.get(InviteeNameId))
        val preparedForm = value.fold(form)(form.fill)

        Future.successful(Ok(view(preparedForm, mode)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate() andThen getData).async {
      implicit request =>

        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) =>
            Future.successful(BadRequest(view(formWithErrors, mode))),

          value => {
            dataCacheConnector.save(request.externalId, InviteeNameId, value).map(
              cacheMap =>
                Redirect(navigator.nextPage(InviteeNameId, mode, UserAnswers(cacheMap)))
            )
          }
        )
    }
}
