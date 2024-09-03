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

package controllers.psp

import com.google.inject.Inject
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaSchemeAuthAction}
import controllers.psa.routes._
import identifiers.{SchemeNameId, SeqAuthorisedPractitionerId}
import models.FeatureToggleName.UpdateClientReference
import models.SchemeReferenceNumber
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.FeatureToggleService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateHelper
import viewmodels.AuthorisedPractitionerViewModel
import views.html.psp.viewPractitioners

import scala.concurrent.{ExecutionContext, Future}

class ViewPractitionersController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             toggleService: FeatureToggleService,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: viewPractitioners,
                                             psaSchemeAction: PsaSchemeAuthAction
                                           )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeNameId and SeqAuthorisedPractitionerId).retrieve.map {
        case schemeName ~ authorisedPractitioners =>
          val authorisedPractitionerViewModelSeq = authorisedPractitioners.map(p =>
            AuthorisedPractitionerViewModel(
              pspName = p.name,
              authorisedBy = p.authorisingPSA.name,
              dateAuthorised = DateHelper.formatDate(p.relationshipStartDate),
              authorisedByLoggedInPsa = request.psaIdOrException.id == p.authorisingPSAID
            )
          )
          val returnCall = PsaSchemeDashboardController.onPageLoad(srn)
          isUpdateClientReferenceEnabled.flatMap { isUpdateClientReference =>
            Future.successful(Ok(view(schemeName, returnCall, authorisedPractitionerViewModelSeq, isUpdateClientReference, srn)))
          }
      }
  }

  private def isUpdateClientReferenceEnabled(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    toggleService.get(UpdateClientReference).map { toggle =>
      toggle.isEnabled
    }
  }

}
