/*
 * Copyright 2019 HM Revenue & Customs
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
import connectors.{AddressLookupConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.AdviserAddressPostcodeLookupFormProvider
import identifiers.TypedIdentifier
import identifiers.invitations.{AdviserAddressPostCodeLookupId, AdviserNameId}
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

import scala.concurrent.{ExecutionContext, Future}

class AdviserAddressPostcodeLookupController @Inject()(val appConfig: FrontendAppConfig,
                                                       val messagesApi: MessagesApi,
                                                       authenticate: AuthAction,
                                                       getData: DataRetrievalAction,
                                                       requireData: DataRequiredAction,
                                                       formProvider: AdviserAddressPostcodeLookupFormProvider,
                                                       @AcceptInvitation navigator: Navigator,
                                                       val addressLookupConnector: AddressLookupConnector,
                                                       val cacheConnector: UserAnswersCacheConnector
                                                      )(implicit val ec: ExecutionContext) extends FrontendController with I18nSupport with Retrievals {


  val form: Form[String] = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      AdviserNameId.retrieve.right.map{ name =>
        Future.successful(Ok(adviserPostcode(appConfig, formProvider(), name)))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold( formWithErrors =>
        AdviserNameId.retrieve.right.map { name =>
          Future.successful(BadRequest(adviserPostcode(appConfig, formWithErrors, name)))
        },
        lookup(AdviserAddressPostCodeLookupId)
      )
  }

  private def lookup(id: TypedIdentifier[Seq[TolerantAddress]])
                    (postcode: String)
                    (implicit request: DataRequest[AnyContent]): Future[Result] = {

    addressLookupConnector.addressLookupByPostCode(postcode).flatMap {

      case Nil =>
        AdviserNameId.retrieve.right.map { name =>
          Future.successful(Ok(adviserPostcode(appConfig, formWithError("messages__error__postcode__lookup__no__results"), name)))
        }

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
      case _ => AdviserNameId.retrieve.right.map { name =>
        Future.successful(BadRequest(adviserPostcode(appConfig, formWithError("messages__error__postcode__lookup__invalid"), name)))
      }
    }
  }

  private def formWithError(message: Message)(implicit request: DataRequest[AnyContent]): Form[String] = {
    form.withError("value", message.resolve)
  }
}
