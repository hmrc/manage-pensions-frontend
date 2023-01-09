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

package controllers.invitations.psa

import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.psa.PensionAdviserAddressListFormProvider
import identifiers.invitations.psa.{AdviserAddressListId, AdviserAddressPostCodeLookupId}
import models.{Mode, NormalMode, TolerantAddress}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AcceptInvitation
import utils.{Navigator, UserAnswers}
import views.html.invitations.psa.pension_adviser_address_list

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionAdviserAddressListController @Inject()(
                                                     authenticate: AuthAction,
                                                     getData: DataRetrievalAction,
                                                     requireData: DataRequiredAction,
                                                     formProvider: PensionAdviserAddressListFormProvider,
                                                     override val messagesApi: MessagesApi,
                                                     val cacheConnector: UserAnswersCacheConnector,
                                                     @AcceptInvitation navigator: Navigator,
                                                     val controllerComponents: MessagesControllerComponents,
                                                     view: pension_adviser_address_list
                                                   )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport {

  def form(addresses: Seq[TolerantAddress]): Form[Int] = formProvider(addresses)

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      AdviserAddressPostCodeLookupId.retrieve.map { addresses =>
        Future.successful(Ok(view(form(addresses), addresses, mode)))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      AdviserAddressPostCodeLookupId.retrieve.map { addresses =>
        formProvider(addresses).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, addresses, mode))),
          addressIndex => {
            val address = addresses(addressIndex).copy(countryOpt = Some("GB"))
            address.toAddress.map(_.toTolerantAddress) match {
              case None =>
                cacheConnector.save(request.externalId, AdviserAddressListId, address).map { _ =>
                  Redirect(controllers.invitations.psa.routes.AdviserManualAddressController.onPageLoad(mode, prepopulated = false))
                }
              case Some(t) =>
                cacheConnector.save(request.externalId, AdviserAddressListId, t).map { json =>
                  Redirect(navigator.nextPage(AdviserAddressListId, NormalMode, UserAnswers(json)))
                }

            }
          }
        )
      }
  }

}
