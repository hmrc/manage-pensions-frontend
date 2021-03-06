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

package controllers.invitations.psp

import com.google.inject.Inject
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.invitations.psp.PspClientReferenceFormProvider
import identifiers.invitations.psp.{PspClientReferenceId, PspNameId}
import models.Mode
import forms.invitations.psp.PspClientReferenceFormProvider
import identifiers.invitations.psp.PspClientReferenceId
import identifiers.invitations.psp.PspNameId
import identifiers.SchemeNameId
import identifiers.SchemeSrnId
import models.invitations.psp.ClientReference
import models.Mode
import models.SchemeReferenceNumber
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthorisePsp
import utils.Navigator
import utils.UserAnswers
import views.html.invitations.psp.pspClientReference

import scala.concurrent.ExecutionContext
import scala.concurrent.Future


class PspClientReferenceController @Inject()(override val messagesApi: MessagesApi,
                                             authenticate: AuthAction,
                                             @AuthorisePsp navigator: Navigator,
                                             dataCacheConnector: UserAnswersCacheConnector,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: PspClientReferenceFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: pspClientReference
                                            )(implicit val ec: ExecutionContext) extends FrontendBaseController with Retrievals with I18nSupport {

  val form: Form[ClientReference] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PspNameId and SchemeSrnId).retrieve.right.map {
        case schemeName ~ pspName ~ srn =>
          val value = request.userAnswers.get(PspClientReferenceId)
          val preparedForm = value.fold(form)(form.fill)

          Future.successful(Ok(view(preparedForm, pspName, mode, schemeName, returnCall(srn))))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
          form.bindFromRequest().fold(
            (formWithErrors: Form[_]) => {
              (SchemeNameId and PspNameId and SchemeSrnId).retrieve.right.map {
                case schemeName ~ pspName ~ srn =>
                  Future.successful(BadRequest(view(formWithErrors, pspName, mode, schemeName, returnCall(srn))))
              }},
            value =>
              dataCacheConnector.save(request.externalId, PspClientReferenceId, value).map(
                cacheMap =>
                  Redirect(navigator.nextPage(PspClientReferenceId, mode, UserAnswers(cacheMap)))
              )
          )

  }

    private def returnCall(srn:String):Call  = controllers.routes.PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))

}
