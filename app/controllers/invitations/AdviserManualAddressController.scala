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

package controllers.invitations

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.AdviserManualAddressFormProvider
import identifiers.invitations.{AdviserAddressId, AdviserAddressListId, AdviserAddressPostCodeLookupId, AdviserNameId}
import javax.inject.Inject
import models.{Address, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendBaseController, FrontendController}
import utils.annotations.AcceptInvitation
import utils.countryOptions.CountryOptions
import utils.{Navigator, UserAnswers}
import views.html.invitations.adviserAddress

import scala.concurrent.{ExecutionContext, Future}

class AdviserManualAddressController @Inject()(
                                                authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                appConfig: FrontendAppConfig,
                                                formProvider: AdviserManualAddressFormProvider,
                                                override val messagesApi: MessagesApi,
                                                countryOptions: CountryOptions,
                                                cacheConnector: UserAnswersCacheConnector,
                                                @AcceptInvitation navigator: Navigator,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: adviserAddress
                                              )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  val form: Form[Address] = formProvider()

  def onPageLoad(mode: Mode, prepopulated: Boolean): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>

      AdviserNameId.retrieve.right.map { name =>

        val prefix = if (prepopulated) {
          "adviser__address__confirm"
        } else {
          "adviser__address"
        }

        val preparedForm = request.userAnswers.get(AdviserAddressId) map form.fill getOrElse {
          request.userAnswers.get(AdviserAddressListId) map { value =>
            form.fill(value.toAddress)
          } getOrElse form
        }

        Future.successful(Ok(view(preparedForm, mode, countryOptions.options, prepopulated, prefix, name)))
      }

  }

  def onSubmit(mode: Mode, prepopulated: Boolean): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>

      val prefix = if (prepopulated) {
        "adviser__address__confirm"
      } else {
        "adviser__address"
      }

      form.bindFromRequest().fold(
        (formWithError: Form[_]) => AdviserNameId.retrieve.right.map { name =>
          Future.successful(BadRequest(view(formWithError, mode, countryOptions.options, prepopulated, prefix, name)))
        },
        address =>
          cacheConnector.remove(request.externalId, AdviserAddressPostCodeLookupId).flatMap { _ =>
            cacheConnector.save(
              request.externalId,
              AdviserAddressId,
              address
            ).map {
              cacheMap =>
                Redirect(navigator.nextPage(AdviserAddressId, mode, UserAnswers(cacheMap)))
            }
          }
      )

  }

}
