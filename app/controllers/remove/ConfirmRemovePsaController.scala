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

package controllers.remove

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.remove.ConfirmRemovePsaFormProvider
import identifiers.SchemeSrnId
import identifiers.invitations.{PSANameId, SchemeNameId}
import identifiers.remove.ConfirmRemovePsaId
import javax.inject.Inject
import models.NormalMode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.RemovePSA
import utils.{Navigator, UserAnswers}
import views.html.remove.confirmRemovePsa

import scala.concurrent.{ExecutionContext, Future}

class ConfirmRemovePsaController @Inject()(
                                            val appConfig: FrontendAppConfig,
                                            val auth: AuthAction,
                                            val messagesApi: MessagesApi,
                                            @RemovePSA navigator: Navigator,
                                            val formProvider: ConfirmRemovePsaFormProvider,
                                            val userAnswersCacheConnector: UserAnswersCacheConnector,
                                            val getData: DataRetrievalAction,
                                            val requireData: DataRequiredAction
                                                     )(implicit val ec: ExecutionContext) extends FrontendController with I18nSupport with Retrievals {

  val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>

      println( "\n1:" + request.userAnswers.get(SchemeSrnId))
      println( "\n2:" + request.userAnswers.get(SchemeNameId))
      println( "\n3:" + request.userAnswers.get(PSANameId))

      (SchemeSrnId and SchemeNameId and PSANameId).retrieve.right.map {
        case srn ~ schemeName ~ psaName =>
          val preparedForm = request.userAnswers.get(ConfirmRemovePsaId).fold(form)(form.fill(_))
          Future.successful(Ok(confirmRemovePsa(appConfig, preparedForm, schemeName, srn, psaName)))
      }
  }

  def onSubmit: Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          (SchemeNameId and SchemeSrnId and PSANameId).retrieve.right.map {
            case schemeName ~ srn ~ psaName =>
              Future.successful(BadRequest(confirmRemovePsa(appConfig, formWithErrors, schemeName, srn, psaName)))
          },
        value => {
          userAnswersCacheConnector.save(request.externalId, ConfirmRemovePsaId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(ConfirmRemovePsaId, NormalMode, UserAnswers(cacheMap)))
          )
        }
      )
  }
}
