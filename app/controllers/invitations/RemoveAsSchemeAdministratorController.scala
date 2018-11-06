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
import connectors.{SchemeDetailsConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.RemoveAsSchemeAdministratorFormProvider
import identifiers.SchemeSrnId
import identifiers.invitations.{RemoveAsSchemeAdministratorId, SchemeNameId}
import javax.inject.Inject
import models.NormalMode
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.RemovePSA
import utils.{Navigator, UserAnswers}
import views.html.invitations.removeAsSchemeAdministrator

import scala.concurrent.Future

class RemoveAsSchemeAdministratorController @Inject()(
                                                       val appConfig: FrontendAppConfig,
                                                       val auth: AuthAction,
                                                       val messagesApi: MessagesApi,
                                                       @RemovePSA navigator: Navigator,
                                                       val formProvider: RemoveAsSchemeAdministratorFormProvider,
                                                       val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                       val getData: DataRetrievalAction,
                                                       val requireData: DataRequiredAction,
                                                       schemeDetailsConnector: SchemeDetailsConnector
                                                     ) extends FrontendController with I18nSupport with Retrievals {

  val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>
      SchemeSrnId.retrieve.right.map { srn =>
        schemeDetailsConnector.getSchemeDetails("srn", srn).flatMap { details =>
          val preparedForm = request.userAnswers.get(RemoveAsSchemeAdministratorId).fold(form)(form.fill(_))
          userAnswersCacheConnector.save(SchemeNameId, details.schemeDetails.name).map { _ =>
            Ok(removeAsSchemeAdministrator(appConfig, preparedForm, details.schemeDetails.name, srn))
          }
        }
      }
  }

  def onSubmit: Action[AnyContent] = (auth andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          (SchemeNameId and SchemeSrnId).retrieve.right.map {
            case schemeName ~ srn =>
              Future.successful(BadRequest(removeAsSchemeAdministrator(appConfig, formWithErrors, schemeName, srn)))
          },
        value => {
          userAnswersCacheConnector.save(request.externalId, RemoveAsSchemeAdministratorId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(RemoveAsSchemeAdministratorId, NormalMode, UserAnswers(cacheMap)))
          )
        }
      )
  }
}
