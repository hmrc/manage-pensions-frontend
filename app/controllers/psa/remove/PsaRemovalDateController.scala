/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.psa.remove

import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import connectors.admin.PsaRemovalConnector
import connectors.scheme.{PensionSchemeVarianceLockConnector, UpdateSchemeCacheConnector}
import controllers.Retrievals
import controllers.actions._
import forms.psa.remove.RemovalDateFormProvider
import identifiers.{AssociatedDateId, SchemeSrnId}
import identifiers.invitations.{PSTRId, SchemeNameId}
import identifiers.psa.PSANameId
import identifiers.psa.remove.PsaRemovalDateId
import models.NormalMode
import models.psa.remove.PsaToBeRemovedFromScheme
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Reads._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateHelper._
import utils.{Navigator, UserAnswers}
import utils.annotations.RemovePSA
import views.html.psa.remove.removalDate

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsaRemovalDateController @Inject()(
                                          appConfig: FrontendAppConfig,
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
                                          view: removalDate
                                        )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def form(schemeOpenDate: LocalDate): Form[LocalDate] =
    formProvider(schemeOpenDate, appConfig.earliestDatePsaRemoval)

  def onPageLoad: Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSANameId and SchemeSrnId and AssociatedDateId).retrieve.right.map {

        case schemeName ~ psaName ~ srn ~ associationDate =>
          Future.successful(Ok(view(form(associationDate), psaName, schemeName, srn, formatDate(associationDate))))

        case _ =>
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSANameId and SchemeSrnId and PSTRId and AssociatedDateId).retrieve.right.map {
        case schemeName ~ psaName ~ srn ~ pstr ~ associationDate =>
          form(associationDate).bindFromRequest().fold(
            (formWithErrors: Form[_]) =>
              Future.successful(BadRequest(view(formWithErrors, psaName, schemeName, srn, formatDate(associationDate)))),
            value =>
              dataCacheConnector.save(request.externalId, PsaRemovalDateId, value).flatMap { cacheMap =>
                psaRemovalConnector.remove(PsaToBeRemovedFromScheme(request.psaIdOrException.id, pstr, value)).flatMap { _ =>
                  val updateDataAndlockRemovalResult = lockConnector.getLockByPsa(request.psaIdOrException.id).map {
                    case Some(lockedSchemeVariance) if lockedSchemeVariance.srn == srn =>
                      updateConnector.removeAll(srn).map(_ => lockConnector.releaseLock(request.psaIdOrException.id, srn))
                    case _ => Future.successful(())
                  }
                  updateDataAndlockRemovalResult.map { _ =>
                    Redirect(navigator.nextPage(PsaRemovalDateId, NormalMode, UserAnswers(cacheMap)))
                  }
                }

              }
          )
      }
  }

}
