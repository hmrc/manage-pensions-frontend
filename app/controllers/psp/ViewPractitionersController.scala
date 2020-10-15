/*
 * Copyright 2020 HM Revenue & Customs
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
import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import identifiers.SeqAuthorisedPractitionerId
import identifiers.SchemeNameId
import identifiers.SchemeSrnId
import models.SchemeReferenceNumber
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.DateHelper
import utils.Navigator
import utils.annotations.Invitation
import viewmodels.AuthorisedPractitionerViewModel
import views.html.psp.viewPractitioners

import scala.concurrent.Future

class ViewPractitionersController @Inject()(appConfig: FrontendAppConfig,
                                            override val messagesApi: MessagesApi,
                                            @Invitation navigator: Navigator,
                                            authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            userAnswersCacheConnector: UserAnswersCacheConnector,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: viewPractitioners
                                         ) extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId and SeqAuthorisedPractitionerId).retrieve.right.map {
        case srn ~ schemeName ~ authorisedPractitioners =>
          val authorisedPractitionerViewModelSeq = authorisedPractitioners.map{ p =>

            AuthorisedPractitionerViewModel(p.name, p.authorisingPSA.name, DateHelper.formatDate(p.relationshipStartDate))
      }
        val returnCall = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber(srn))
        Future.successful(Ok(view(schemeName, returnCall, authorisedPractitionerViewModelSeq)))
      }
  }

}
