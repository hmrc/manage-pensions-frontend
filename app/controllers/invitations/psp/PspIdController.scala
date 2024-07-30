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

package controllers.invitations.psp

import com.google.inject.Inject
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.psa.routes._
import forms.invitations.psp.PspIdFormProvider
import identifiers.invitations.psp.{PspId, PspNameId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{Mode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthorisePsp
import utils.{Navigator, UserAnswers}
import views.html.invitations.psp.pspId

import scala.concurrent.{ExecutionContext, Future}


class PspIdController @Inject()(
                                 override val messagesApi: MessagesApi,
                                 authenticate: AuthAction,
                                 @AuthorisePsp navigator: Navigator,
                                 dataCacheConnector: UserAnswersCacheConnector,
                                 getData: DataRetrievalAction,
                                 requireData: DataRequiredAction,
                                 formProvider: PspIdFormProvider,
                                 val controllerComponents: MessagesControllerComponents,
                                 view: pspId,
                                 psaSchemeAuthAction: PsaPspSchemeAuthAction
                               )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode, srn: SchemeReferenceNumber): Action[AnyContent] =
                (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>

      (SchemeNameId and PspNameId and SchemeSrnId).retrieve.map {
        case schemeName ~ pspName ~ srn =>
          val value = request.userAnswers.get(PspId)
          val preparedForm = value.fold(form)(form.fill)

          Future.successful(Ok(view(preparedForm, pspName, mode, schemeName, returnCall(srn))))
      }
  }

  def onSubmit(mode: Mode, srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          (SchemeNameId and PspNameId and SchemeSrnId).retrieve.map {
            case schemeName ~ pspName ~ srn =>
              Future.successful(BadRequest(view(formWithErrors, pspName, mode, schemeName, returnCall(srn))))
          },
        value =>
          dataCacheConnector.save(request.externalId, PspId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(PspId, mode, UserAnswers(cacheMap)))
          )
      )

  }

  private def returnCall(srn: String): Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))

}
