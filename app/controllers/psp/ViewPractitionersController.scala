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

package controllers.psp

import com.google.inject.Inject
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.{SchemeNameId, SchemeSrnId, SeqAuthorisedPractitionerId}
import models.SchemeReferenceNumber
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateHelper
import viewmodels.AuthorisedPractitionerViewModel
import views.html.psp.viewPractitioners

import scala.concurrent.Future

class ViewPractitionersController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: viewPractitioners
                                           )
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def onPageLoad(): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId and SeqAuthorisedPractitionerId).retrieve.right.map {
        case srn ~ schemeName ~ authorisedPractitioners =>
          val authorisedPractitionerViewModelSeq = authorisedPractitioners.map(p =>
            AuthorisedPractitionerViewModel(
              pspName = p.name,
              authorisedBy = p.authorisingPSA.name,
              dateAuthorised = DateHelper.formatDate(p.relationshipStartDate),
              authorisedByLoggedInPsa = request.psaIdOrException.id == p.authorisingPSAID
            )
          )
          val returnCall = controllers.routes.PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))
          Future.successful(Ok(view(schemeName, returnCall, authorisedPractitionerViewModelSeq)))
      }
  }

}
