/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.{AddressLookupConnector, DataCacheConnector}
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.AdviserAddressPostcodeLookupFormProvider
import identifiers.TypedIdentifier
import identifiers.invitations.AdviserAddressPostCodeLookupId
import javax.inject.Inject
import models.requests.DataRequest
import models.{NormalMode, TolerantAddress}
import play.api.data.Form
import play.api.i18n._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.AcceptInvitation
import utils.{Navigator, UserAnswers}
import viewmodels.Message
import views.html.invitations.adviserPostcode

import scala.concurrent.Future

class AdviserAddressPostcodeLookupController @Inject()(val appConfig: FrontendAppConfig,
                                                       val messagesApi: MessagesApi,
                                                       authenticate: AuthAction,
                                                       getData: DataRetrievalAction,
                                                       requireData: DataRequiredAction,
                                                       formProvider: AdviserAddressPostcodeLookupFormProvider,
                                                       @AcceptInvitation navigator: Navigator,
                                                       val addressLookupConnector: AddressLookupConnector,
                                                       val cacheConnector: DataCacheConnector
                                                      ) extends FrontendController with I18nSupport {


  val form: Form[String] = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>
      Ok(adviserPostcode(appConfig, formProvider()))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold( formWithErrors =>
        Future.successful(BadRequest(adviserPostcode(appConfig, formWithErrors))),
        lookup(AdviserAddressPostCodeLookupId)
      )
  }

  private def lookup(id: TypedIdentifier[Seq[TolerantAddress]])
                    (postcode: String)
                    (implicit request: DataRequest[AnyContent]): Future[Result] = {

    addressLookupConnector.addressLookupByPostCode(postcode).flatMap {

      case Nil => Future.successful(Ok(adviserPostcode(appConfig, formWithError("messages__error__postcode__lookup__no__results"))))

      case addresses =>
        cacheConnector.save(
          request.externalId,
          id,
          addresses
        ) map {
          json =>
            Redirect(navigator.nextPage(id, NormalMode, UserAnswers(json)))
        }
    } recoverWith {
      case _ =>
        Future.successful(BadRequest(adviserPostcode(appConfig, formWithError("messages__error__postcode__lookup__invalid"))))
    }
  }

  private def formWithError(message: Message)(implicit request: DataRequest[AnyContent]): Form[String] = {
    form.withError("value", message.resolve)
  }
}
