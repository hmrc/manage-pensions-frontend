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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.DeclarationFormProvider
import identifiers.invitations.{HaveYouEmployedPensionAdviserId, IsMasterTrustId}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.invitations.declaration

import scala.concurrent.Future

class DeclarationController @Inject()(
                                       appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
                                       formProvider: DeclarationFormProvider,
                                       auth: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction
                                     ) extends FrontendController with I18nSupport with Retrievals {
  val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>
      (HaveYouEmployedPensionAdviserId and IsMasterTrustId).retrieve.right.map {
        case havePensionAdviser ~ isMasterTrust =>
          Future.successful(Ok(declaration(appConfig, havePensionAdviser, isMasterTrust, form)))
      }
  }
}
