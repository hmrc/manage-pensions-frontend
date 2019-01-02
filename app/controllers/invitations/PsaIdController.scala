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
import controllers.actions._
import forms.invitations.PsaIdFromProvider
import identifiers.invitations.{InviteeNameId, InviteePSAId}
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.Invitation
import utils.{Navigator, UserAnswers}
import views.html.invitations.psaId

import scala.concurrent.{ExecutionContext, Future}


class PsaIdController @Inject()(appConfig: FrontendAppConfig,
                                override val messagesApi: MessagesApi,
                                authenticate: AuthAction,
                                @Invitation navigator: Navigator,
                                dataCacheConnector: UserAnswersCacheConnector,
                                getData: DataRetrievalAction,
                                requireData: DataRequiredAction,
                                formProvider: PsaIdFromProvider)(implicit val ec: ExecutionContext) extends FrontendController with Retrievals with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>

      InviteeNameId.retrieve.right.map {
        psaName =>
          val value = request.userAnswers.get(InviteePSAId)
          val preparedForm = value.fold(form)(form.fill)

          Future.successful(Ok(psaId(appConfig, preparedForm, psaName, mode)))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
          form.bindFromRequest().fold(
            (formWithErrors: Form[_]) =>
              InviteeNameId.retrieve.right.map {
                psaName =>
                  Future.successful(BadRequest(psaId(appConfig, formWithErrors, psaName, mode)))
              },
            value =>
              dataCacheConnector.save(request.externalId, InviteePSAId, value).map(
                cacheMap =>
                  Redirect(navigator.nextPage(InviteePSAId, mode, UserAnswers(cacheMap)))
              )
          )

  }
}
