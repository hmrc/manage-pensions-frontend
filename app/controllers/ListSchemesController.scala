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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.{ListOfSchemesConnector, MinimalPsaConnector, UserAnswersCacheConnector}
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.PSANameId
import models.SchemeDetail
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.list_schemes

import scala.concurrent.{ExecutionContext, Future}

class ListSchemesController @Inject()(
                                       val appConfig: FrontendAppConfig,
                                       val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       listSchemesConnector: ListOfSchemesConnector,
                                       minimalPsaConnector: MinimalPsaConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector
                                     )(implicit val ec: ExecutionContext) extends FrontendController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      listSchemesConnector.getListOfSchemes(request.psaId.id).flatMap {
        listOfSchemes =>
          val schemes = listOfSchemes.schemeDetail.getOrElse(List.empty[SchemeDetail])
          request.userAnswers.get(PSANameId) match {
            case None =>
              minimalPsaConnector.getPsaNameFromPsaID(request.psaId.id).flatMap(_.map { name =>
                 userAnswersCacheConnector.save(request.externalId, PSANameId, name).map { _ =>
                  Ok(list_schemes(appConfig, schemes, name))
                }}.getOrElse(
                Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
                )
              )
            case Some(name) => Future.successful(Ok(list_schemes(appConfig, schemes, name)))
          }
      }
  }
}
