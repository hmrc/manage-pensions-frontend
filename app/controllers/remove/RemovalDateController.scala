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

package controllers.remove

import java.time.LocalDate

import config.FrontendAppConfig
import connectors.{PensionSchemeVarianceLockConnector, PsaRemovalConnector, UpdateSchemeCacheConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions._
import forms.remove.RemovalDateFormProvider
import identifiers.invitations.{PSANameId, PSTRId, SchemeNameId}
import identifiers.remove.RemovalDateId
import identifiers.{AssociatedDateId, SchemeSrnId}
import javax.inject.Inject
import models.{NormalMode, PsaToBeRemovedFromScheme}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Reads._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendBaseController, FrontendController}
import utils.DateHelper._
import utils.annotations.RemovePSA
import utils.{Navigator, UserAnswers}
import views.html.remove.removalDate

import scala.concurrent.{ExecutionContext, Future}

class RemovalDateController @Inject()(appConfig: FrontendAppConfig,
                                      override val messagesApi: MessagesApi,
                                      dataCacheConnector: UserAnswersCacheConnector,
                                      @RemovePSA navigator: Navigator,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: RemovalDateFormProvider,
                                      psaRemovalConnector: PsaRemovalConnector,
                                      updateConnector: UpdateSchemeCacheConnector,
                                      lockConnector: PensionSchemeVarianceLockConnector,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: removalDate)(
                                       implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {

  private def form(schemeOpenDate: LocalDate) = formProvider(schemeOpenDate, appConfig.earliestDatePsaRemoval)

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSANameId and SchemeSrnId and AssociatedDateId).retrieve.right.map {

        case schemeName ~ psaName ~ srn ~ associationDate =>
          Future.successful(Ok(view(form(associationDate), psaName, schemeName, srn, formatDate(associationDate))))

        case _ =>
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSANameId and SchemeSrnId and PSTRId and AssociatedDateId).retrieve.right.map {
        case schemeName ~ psaName ~ srn ~ pstr ~ associationDate =>
          form(associationDate).bindFromRequest().fold(
            (formWithErrors: Form[_]) =>
              Future.successful(BadRequest(view(formWithErrors, psaName, schemeName, srn, formatDate(associationDate)))),
            value =>
              dataCacheConnector.save(request.externalId, RemovalDateId, value).flatMap { cacheMap =>
                psaRemovalConnector.remove(PsaToBeRemovedFromScheme(request.psaId.id, pstr, value)).flatMap { _ =>
                  val updateDataAndlockRemovalResult = lockConnector.getLockByPsa(request.psaId.id).map {
                    case Some(lockedSchemeVariance) if lockedSchemeVariance.srn == srn =>
                        updateConnector.removeAll(srn).map(_ => lockConnector.releaseLock(request.psaId.id, srn))
                    case _ => Future.successful(())
                  }
                  updateDataAndlockRemovalResult.map { _ =>
                    Redirect(navigator.nextPage(RemovalDateId, NormalMode, UserAnswers(cacheMap)))
                  }
                }

              }
          )
      }
  }

}
