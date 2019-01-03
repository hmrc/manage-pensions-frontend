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

package controllers

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import controllers.actions._
import forms.DeleteSchemeFormProvider
import javax.inject.Inject
import models.requests.OptionalDataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.PensionsSchemeCache
import views.html.deleteScheme

import scala.concurrent.{ExecutionContext, Future}

class DeleteSchemeController @Inject()(
                                        appConfig: FrontendAppConfig,
                                        override val messagesApi: MessagesApi,
                                        @PensionsSchemeCache dataCacheConnector: UserAnswersCacheConnector,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: DeleteSchemeFormProvider
                                      )(implicit val ec: ExecutionContext) extends FrontendController with I18nSupport with Retrievals {

  private val form: Form[Boolean] = formProvider()
  private lazy val overviewPage = Redirect(routes.SchemesOverviewController.onPageLoad())


  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      getSchemeName { schemeName =>
              Future.successful(Ok(deleteScheme(appConfig, form, schemeName)))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      getSchemeName { schemeName =>
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) =>
            Future.successful(BadRequest(deleteScheme(appConfig, formWithErrors, schemeName))),
          {
            case true => dataCacheConnector.removeAll(request.externalId).map {
              _ =>
                overviewPage
            }
            case false => Future.successful(overviewPage)
          }
        )
      }
  }

  private def getSchemeName(f: String => Future[Result])
                           (implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    dataCacheConnector.fetch(request.externalId).flatMap {
      case None => Future.successful(overviewPage)
      case Some(data) =>
        (data \ "schemeDetails" \ "schemeName").validate[String] match {
          case JsSuccess(name, _) => f(name)
          case JsError(_) => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
        }
    }
  }
}
