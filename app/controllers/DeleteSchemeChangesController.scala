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

package controllers

import config.FrontendAppConfig
import connectors.scheme.{PensionSchemeVarianceLockConnector, UpdateSchemeCacheConnector}
import controllers.actions._
import forms.DeleteSchemeChangesFormProvider
import identifiers.PSANameId
import javax.inject.Inject
import models.requests.DataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.deleteSchemeChanges

import scala.concurrent.{ExecutionContext, Future}

class DeleteSchemeChangesController @Inject()(
                                               appConfig: FrontendAppConfig,
                                               override val messagesApi: MessagesApi,
                                               updateConnector: UpdateSchemeCacheConnector,
                                               lockConnector: PensionSchemeVarianceLockConnector,
                                               authenticate: AuthAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               formProvider: DeleteSchemeChangesFormProvider,
                                               val controllerComponents: MessagesControllerComponents,
                                               view: deleteSchemeChanges
                                             )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  private lazy val overviewPage = Redirect(routes.SchemesOverviewController.onPageLoad())
  private lazy val postCall = routes.DeleteSchemeChangesController.onSubmit _
  private val form: Form[Boolean] = formProvider()

  def onPageLoad(srn: String): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>

        getSchemeName(srn) { (psaName, schemeName) =>
          Future.successful(Ok(view(form, schemeName, postCall(srn), psaName)))
        }

  }

  private def getSchemeName(srn: String)(block: (String, String) => Future[Result])
                           (implicit request: DataRequest[AnyContent], hc: HeaderCarrier): Future[Result] =
    request.userAnswers.get(PSANameId).map { psaName =>
      updateConnector.fetch(srn).flatMap {
        case None => Future.successful(overviewPage)
        case Some(data) =>
          (data \ "schemeName").validate[String] match {
            case JsSuccess(name, _) =>
              block(psaName, name)
            case JsError(_) => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }
      }
    }.getOrElse(Future.successful(overviewPage))

  def onSubmit(srn: String): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      getSchemeName(srn) { (psaName, schemeName) =>
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) =>
            Future.successful(BadRequest(view(formWithErrors, schemeName, postCall(srn), psaName))),
          {
            case true =>
              updateConnector.removeAll(srn).flatMap { _ =>
                lockConnector.releaseLock(request.psaId.id, srn).map(_ => overviewPage)
              }
            case false => Future.successful(overviewPage)
          }
        )
      }
  }
}
