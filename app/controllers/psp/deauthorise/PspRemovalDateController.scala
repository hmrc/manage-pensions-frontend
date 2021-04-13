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

package controllers.psp.deauthorise

import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.remove.psp.PspRemovalDateFormProvider
import identifiers.remove.psp
import identifiers.remove.psp.{PspDetailsId, PspRemovalDateId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{Index, NormalMode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Reads._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateHelper._
import utils.annotations.RemovePSP
import utils.{Navigator, UserAnswers}
import viewmodels.Message
import views.html.remove.psp.pspRemovalDate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PspRemovalDateController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          userAnswersCacheConnector: UserAnswersCacheConnector,
                                          @RemovePSP navigator: Navigator,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: PspRemovalDateFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: pspRemovalDate
                                        )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  private def earliestDateError(date: String) = Message("messages__pspRemoval_date_error__before_earliest_date", date)

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate() andThen getData andThen requireData).async {
      implicit request =>

        (SchemeSrnId and SchemeNameId and PspDetailsId(index)).retrieve.right.map {
          case srn ~ schemeName ~ pspDetails =>
            if (pspDetails.authorisingPSAID == request.psaIdOrException.id) {
              Future.successful(Ok(view(
                form = formProvider(
                  relationshipStartDate = pspDetails.relationshipStartDate,
                  earliestDateError = earliestDateError(formatDate(pspDetails.relationshipStartDate)).resolve
                ),
                pspName = pspDetails.name,
                schemeName = schemeName,
                srn = srn,
                relationshipStartDate = formatDate(pspDetails.relationshipStartDate),
                index = index
              )))
            } else {
              Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
            }
        }
    }

  def onSubmit(index: Index): Action[AnyContent] =
    (authenticate() andThen getData andThen requireData).async {
      implicit request =>
        (SchemeSrnId and SchemeNameId and psp.PspDetailsId(index)).retrieve.right.map {
          case srn ~ schemeName ~ pspDetails =>
            formProvider(
              relationshipStartDate = pspDetails.relationshipStartDate,
              earliestDateError = earliestDateError(formatDate(pspDetails.relationshipStartDate)).resolve
            ).bindFromRequest().fold(
              (formWithErrors: Form[_]) =>
                Future.successful(BadRequest(view(
                  form = formWithErrors,
                  pspName = pspDetails.name,
                  schemeName = schemeName,
                  srn = srn,
                  relationshipStartDate = formatDate(pspDetails.relationshipStartDate),
                  index = index
                ))),
              value =>
                userAnswersCacheConnector.save(request.externalId, PspRemovalDateId(index), value).map {
                  cacheMap =>
                    Redirect(navigator.nextPage(psp.PspRemovalDateId(index), NormalMode, UserAnswers(cacheMap)))
                }
            )
        }
    }
}
