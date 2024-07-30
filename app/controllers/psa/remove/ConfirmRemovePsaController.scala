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

package controllers.psa.remove

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaPspSchemeAuthAction}
import forms.psa.remove.ConfirmRemovePsaFormProvider
import identifiers.SchemeSrnId
import identifiers.invitations.SchemeNameId
import identifiers.psa.PSANameId
import identifiers.psa.remove.ConfirmRemovePsaId
import models.{NormalMode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.RemovePSA
import utils.{Navigator, UserAnswers}
import views.html.psa.remove.confirmRemovePsa

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmRemovePsaController @Inject()(
                                            val appConfig: FrontendAppConfig,
                                            val auth: AuthAction,
                                            override val messagesApi: MessagesApi,
                                            @RemovePSA navigator: Navigator,
                                            val formProvider: ConfirmRemovePsaFormProvider,
                                            val userAnswersCacheConnector: UserAnswersCacheConnector,
                                            val getData: DataRetrievalAction,
                                            val requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: confirmRemovePsa,
                                            psaPspSchemeAuthAction: PsaPspSchemeAuthAction
                                          )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = (auth() andThen getData andThen psaPspSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId and PSANameId).retrieve.map {
        case srn ~ schemeName ~ psaName =>
          val preparedForm = request.userAnswers.get(ConfirmRemovePsaId).fold(form)(form.fill)
          Future.successful(Ok(view(preparedForm, schemeName, srn, psaName)))
      }
  }

  def onSubmit(srn: SchemeReferenceNumber): Action[AnyContent] = (auth() andThen getData andThen psaPspSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          (SchemeNameId and SchemeSrnId and PSANameId).retrieve.map {
            case schemeName ~ srn ~ psaName =>
              Future.successful(BadRequest(view(formWithErrors, schemeName, srn, psaName)))
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
