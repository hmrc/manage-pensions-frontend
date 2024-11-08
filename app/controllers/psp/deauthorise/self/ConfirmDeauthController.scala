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

package controllers.psp.deauthorise.self

import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PspSchemeAuthAction}
import forms.psp.deauthorise.ConfirmDeauthPspFormProvider
import identifiers.psp.PSPNameId
import identifiers.psp.deauthorise.self.ConfirmDeauthId
import identifiers.SchemeNameId
import models.AuthEntity.PSP
import models.{NormalMode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.PspSelfDeauth
import utils.{Navigator, UserAnswers}
import views.html.psp.deauthorisation.self.confirmDeauth

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmDeauthController @Inject()(val auth: AuthAction,
                                        val getData: DataRetrievalAction,
                                        val requireData: DataRequiredAction,
                                        override val messagesApi: MessagesApi,
                                        @PspSelfDeauth navigator: Navigator,
                                        val formProvider: ConfirmDeauthPspFormProvider,
                                        val userAnswersCacheConnector: UserAnswersCacheConnector,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: confirmDeauth,
                                        pspSchemeAuthAction: PspSchemeAuthAction
                                       )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = (auth(PSP) andThen getData andThen pspSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSPNameId).retrieve.map {
        case schemeName ~ pspName =>
          val preparedForm = request.userAnswers.get(ConfirmDeauthId).fold(form)(form.fill)
          Future.successful(Ok(view(preparedForm, schemeName, srn, pspName)))
      }
  }

  def onSubmit(srn: SchemeReferenceNumber): Action[AnyContent] =
              (auth(PSP) andThen getData andThen pspSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          (SchemeNameId and PSPNameId).retrieve.map {
            case schemeName ~ pspName =>
              Future.successful(BadRequest(view(formWithErrors, schemeName, srn, pspName)))
          },
        value =>
          userAnswersCacheConnector.save(request.externalId, ConfirmDeauthId, value).map { cacheMap =>
            Redirect(navigator.nextPage(ConfirmDeauthId, NormalMode, UserAnswers(cacheMap)))
          }
      )
  }
}
