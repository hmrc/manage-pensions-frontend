/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.invitations.psp.routes._
import controllers.psa.routes._
import forms.invitations.psp.PspClientReferenceFormProvider
import identifiers.invitations.psp.{PspClientReferenceId, PspNameId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{Mode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthorisePsp
import utils.{Navigator, UserAnswers}
import views.html.invitations.psp.pspClientReference

import scala.concurrent.{ExecutionContext, Future}


class PspClientReferenceController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              authenticate: AuthAction,
                                              @AuthorisePsp navigator: Navigator,
                                              dataCacheConnector: UserAnswersCacheConnector,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              formProvider: PspClientReferenceFormProvider,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: pspClientReference
                                            )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PspNameId and SchemeSrnId).retrieve.map {
        case schemeName ~ pspName ~ srn =>
          val value = request.userAnswers.get(PspClientReferenceId)
          val preparedForm = value.fold(form)(form.fill)

          Future.successful(Ok(view(preparedForm, pspName, mode, schemeName, returnCall(srn), PspClientReferenceController.onSubmit(mode))))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) => {
          (SchemeNameId and PspNameId and SchemeSrnId).retrieve.map {
            case schemeName ~ pspName ~ srn =>
              Future.successful(BadRequest(view(formWithErrors, pspName, mode, schemeName, returnCall(srn), PspClientReferenceController.onSubmit(mode))))
          }
        },
        value =>
          dataCacheConnector.save(request.externalId, PspClientReferenceId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(PspClientReferenceId, mode, UserAnswers(cacheMap)))
          )
      )

  }

  private def returnCall(srn: String): Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))

}
