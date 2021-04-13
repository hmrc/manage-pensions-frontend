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

package controllers.psp.deauthorise.self

import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.psp.deauthorise.ConfirmRemovePspFormProvider
import identifiers.psp.PSPNameId
import identifiers.remove.psp.selfRemoval.ConfirmRemovalId
import identifiers.{SchemeNameId, SchemeSrnId}

import javax.inject.Inject
import models.AuthEntity.PSP
import models.NormalMode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.PspSelfRemoval
import utils.{Navigator, UserAnswers}
import views.html.remove.psp.selfRemoval.confirmRemoval

import scala.concurrent.{ExecutionContext, Future}

class ConfirmRemovalController @Inject()( val auth: AuthAction,
                                          val getData: DataRetrievalAction,
                                          val requireData: DataRequiredAction,
                                          override val messagesApi: MessagesApi,
                                          @PspSelfRemoval navigator: Navigator,
                                          val formProvider: ConfirmRemovePspFormProvider,
                                          val userAnswersCacheConnector: UserAnswersCacheConnector,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: confirmRemoval
                                          )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = (auth(PSP) andThen getData andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId and PSPNameId).retrieve.right.map {
        case srn ~ schemeName ~ pspName =>
          val preparedForm = request.userAnswers.get(ConfirmRemovalId).fold(form)(form.fill)
          Future.successful(Ok(view(preparedForm, schemeName, srn, pspName)))
      }
  }

  def onSubmit: Action[AnyContent] = (auth(PSP) andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          (SchemeNameId and SchemeSrnId and PSPNameId).retrieve.right.map {
            case schemeName ~ srn ~ pspName =>
              Future.successful(BadRequest(view(formWithErrors, schemeName, srn, pspName)))
        },
        value =>
          userAnswersCacheConnector.save(request.externalId, ConfirmRemovalId, value).map { cacheMap =>
            Redirect(navigator.nextPage(ConfirmRemovalId, NormalMode, UserAnswers(cacheMap)))
          }
      )
  }
}
