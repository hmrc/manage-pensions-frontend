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
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.AdviserManualAddressFormProvider
import javax.inject.Inject
import models.{Address, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.CountryOptions
import views.html.invitations.adviserAddress

class AdviserManualAddressController @Inject()(
                                              authenticate: AuthAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              appConfig: FrontendAppConfig,
                                              formProvider: AdviserManualAddressFormProvider,
                                              val messagesApi: MessagesApi,
                                              countryOptions: CountryOptions
                                              ) extends FrontendController with I18nSupport {

  val form: Form[Address] = formProvider()

  def onPageLoad(mode: Mode, prepopulated: Boolean): Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>
      val prefix = if(prepopulated){
        "adviser__address__confirm"
      } else {
        "adviser__address"
      }
      Ok(adviserAddress(appConfig, form, mode, countryOptions.options, prepopulated, prefix))
  }

  def onSubmit(mode: Mode, prepopulated: Boolean): Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>
      ???
  }

}
