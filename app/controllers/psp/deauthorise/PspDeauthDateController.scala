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

package controllers.psp.deauthorise

import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.psp.deauthorise.PspDeauthDateFormProvider
import identifiers.psp.deauthorise
import identifiers.psp.deauthorise.{PspDeauthDateId, PspDetailsId}
import identifiers.SchemeNameId
import models.{Index, NormalMode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Reads._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateHelper._
import utils.annotations.DeauthorisePSP
import utils.{Navigator, UserAnswers}
import viewmodels.Message
import views.html.psp.deauthorisation.pspDeauthDate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PspDeauthDateController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         userAnswersCacheConnector: UserAnswersCacheConnector,
                                         @DeauthorisePSP navigator: Navigator,
                                         authenticate: AuthAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: PspDeauthDateFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: pspDeauthDate,
                                         psaSchemeAuthAction: PsaSchemeAuthAction
                                       )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  private def earliestDateError(date: String) = Message("messages__pspDeauth_date_error__before_earliest_date", date)

  def onPageLoad(index: Index, srn: SchemeReferenceNumber): Action[AnyContent] =
    (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
      implicit request =>

        (SchemeNameId and deauthorise.PspDetailsId(index)).retrieve.map {
          case schemeName ~ pspDetails =>
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
              Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
            }
        }
    }

  def onSubmit(index: Index, srn: SchemeReferenceNumber): Action[AnyContent] =
    (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
      implicit request =>
        (SchemeNameId and PspDetailsId(index)).retrieve.map {
          case schemeName ~ pspDetails =>
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
                userAnswersCacheConnector.save(request.externalId, deauthorise.PspDeauthDateId(index), value).map {
                  cacheMap =>
                    Redirect(navigator.nextPage(PspDeauthDateId(index), NormalMode, UserAnswers(cacheMap)))
                }
            )
        }
    }
}
