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

package controllers.invitations

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import forms.invitations.PensionAdviserAddressListFormProvider
import identifiers.invitations.AdviserAddressListId
import identifiers.invitations.AdviserAddressPostCodeLookupId
import javax.inject.Inject
import models.Mode
import models.NormalMode
import models.TolerantAddress
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.AcceptInvitation
import utils.Navigator
import utils.UserAnswers
import views.html.invitations.pension_adviser_address_list

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PensionAdviserAddressListController @Inject()(
                                                     appConfig: FrontendAppConfig,
                                                     authenticate: AuthAction,
                                                     getData: DataRetrievalAction,
                                                     requireData: DataRequiredAction,
                                                     formProvider: PensionAdviserAddressListFormProvider,
                                                     override val messagesApi: MessagesApi,
                                                     val cacheConnector: UserAnswersCacheConnector,
                                                     @AcceptInvitation navigator: Navigator,
                                                     val controllerComponents: MessagesControllerComponents,
                                                     view: pension_adviser_address_list
                                                   )(implicit val ec: ExecutionContext) extends FrontendBaseController with Retrievals with I18nSupport {

  def form(addresses: Seq[TolerantAddress]): Form[Int] = formProvider(addresses)

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request => AdviserAddressPostCodeLookupId.retrieve.right.map { addresses =>
        Future.successful(Ok(view(form(addresses), addresses, mode)))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      AdviserAddressPostCodeLookupId.retrieve.right.map { addresses =>
        formProvider(addresses).bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, addresses, mode))),
          addressIndex => {
            val address = addresses(addressIndex).copy(country = Some("GB"))

            cacheConnector.save(request.externalId, AdviserAddressListId, address).map { json =>
              Redirect(navigator.nextPage(AdviserAddressListId, NormalMode, UserAnswers(json)))
            }
          }
        )
      }
  }

}
