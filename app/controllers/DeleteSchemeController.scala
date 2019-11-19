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
import connectors.{MinimalPsaConnector, UserAnswersCacheConnector}
import controllers.actions._
import forms.DeleteSchemeFormProvider
import javax.inject.Inject
import models.requests.OptionalDataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsLookupResult, JsSuccess, JsValue}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendBaseController, FrontendController}
import utils.annotations.PensionsSchemeCache
import views.html.deleteScheme

import scala.concurrent.{ExecutionContext, Future}

class DeleteSchemeController @Inject()(
                                        appConfig: FrontendAppConfig,
                                        override val messagesApi: MessagesApi,
                                        @PensionsSchemeCache dataCacheConnector: UserAnswersCacheConnector,
                                        minimalPsaConnector: MinimalPsaConnector,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: DeleteSchemeFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: deleteScheme
                                      )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  private val form: Form[Boolean] = formProvider()
  private lazy val overviewPage = Redirect(routes.SchemesOverviewController.onPageLoad())


  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      getSchemeName { (schemeName, psaName) =>
              Future.successful(Ok(view(form, schemeName, psaName)))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      getSchemeName { (schemeName, psaName) =>
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) =>
            Future.successful(BadRequest(view(formWithErrors, schemeName, psaName))),
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

  private def getSchemeName(f: (String, String) => Future[Result])
                           (implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {

    dataCacheConnector.fetch(request.externalId).flatMap {
      case None => Future.successful(overviewPage)
      case Some(data) =>
        (data \ "schemeName").validate[String] match {
          case JsSuccess(schemeName, _) =>
            minimalPsaConnector.getPsaNameFromPsaID(request.psaId.id).flatMap(_.map{ psaName =>
              f(schemeName, psaName)
            }.getOrElse(Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))))
          case JsError(_) => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
        }
    }
  }
}
