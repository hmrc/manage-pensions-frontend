/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.invitations.psp

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.actions._
import forms.invitations.psp.PspNameFormProvider
import identifiers.invitations.psp.PspNameId
import javax.inject.Inject
import models.Mode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.Invitation
import utils.{Navigator, UserAnswers}
import views.html.invitations.psp.pspName

import scala.concurrent.{ExecutionContext, Future}

class PspNameController @Inject()(appConfig: FrontendAppConfig,
                                  override val messagesApi: MessagesApi,
                                  dataCacheConnector: UserAnswersCacheConnector,
                                  @Invitation navigator: Navigator,
                                  authenticate: AuthAction,
                                  getData: DataRetrievalAction,
                                  formProvider: PspNameFormProvider,
                                  val controllerComponents: MessagesControllerComponents,
                                  view: pspName
                                 )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

      val value = request.userAnswers.flatMap(_.get(PspNameId))
      val preparedForm = value.fold(form)(form.fill)

      Future.successful(Ok(view(preparedForm, mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        (value) => {
          dataCacheConnector.save(request.externalId, PspNameId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(PspNameId, mode, UserAnswers(cacheMap)))
          )
        }
      )
  }
}
