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

package controllers.remove.pspSelfRemoval

import java.time.LocalDate

import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.remove.PspRemovalDateFormProvider
import identifiers.remove.pspSelfRemoval.RemovalDateId
import identifiers.{AuthorisedPractitionerId, SchemeNameId, SchemeSrnId}
import javax.inject.Inject
import models.AuthEntity.PSP
import models.NormalMode
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Reads._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.DateHelper._
import utils.annotations.PspSelfRemoval
import utils.{Navigator, UserAnswers}
import views.html.remove.pspSelfRemoval.removalDate

import scala.concurrent.{ExecutionContext, Future}

class RemovalDateController @Inject()(override val messagesApi: MessagesApi,
                                      dataCacheConnector: UserAnswersCacheConnector,
                                      @PspSelfRemoval navigator: Navigator,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: PspRemovalDateFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: removalDate)(
                                       implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  private def form(relationshipStartDate: LocalDate)(implicit messages: Messages): Form[LocalDate] =
    formProvider(relationshipStartDate, messages("messages__pspRemoval_date_error__before_earliest_date", formatDate(relationshipStartDate)))

  def onPageLoad: Action[AnyContent] = (authenticate(PSP) andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and SchemeSrnId and AuthorisedPractitionerId).retrieve.right.map {
        case schemeName ~ srn ~ psp =>
            val authDate: LocalDate = psp.relationshipStartDate
            val preparedForm = request.userAnswers.get(RemovalDateId).fold(form(authDate))(form(authDate).fill)
            Future.successful(Ok(view(preparedForm, schemeName, srn, formatDate(authDate))))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate(PSP) andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and SchemeSrnId and AuthorisedPractitionerId).retrieve.right.map {
        case schemeName ~ srn ~ psp =>

            val authDate: LocalDate = psp.relationshipStartDate

            form(authDate).bindFromRequest().fold(
              (formWithErrors: Form[_]) =>
                Future.successful(BadRequest(view(formWithErrors, schemeName, srn, formatDate(authDate)))),

              value =>
                dataCacheConnector.save(request.externalId, RemovalDateId, value).map { cacheMap =>
                  Redirect(navigator.nextPage(RemovalDateId, NormalMode, UserAnswers(cacheMap)))
                }
            )

      }
  }

}
