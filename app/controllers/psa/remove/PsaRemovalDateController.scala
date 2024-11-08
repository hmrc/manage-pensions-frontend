/*
 * Copyright 2024 HM Revenue & Customs
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
import identifiers.invitations.{PSTRId, SchemeNameId}
import identifiers.psa.remove.PsaRemovalDateId
import identifiers.psa.{ListOfPSADetailsId, PSANameId}
import identifiers.AssociatedDateId
import models.{NormalMode, SchemeReferenceNumber}
import models.psa.remove.PsaToBeRemovedFromScheme
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Reads._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateHelper._
import utils.annotations.RemovePSA
import utils.{DateHelper, Navigator, UserAnswers}
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
                                          view: removalDate,
                                          psaSchemeAuthAction: PsaSchemeAuthAction
                                        )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def form(schemeOpenDate: LocalDate): Form[LocalDate] =
    formProvider(schemeOpenDate, appConfig.earliestDatePsaRemoval)

  private def getRelationshipDate(ua: UserAnswers, psaId: String): Option[String] = {
    ua.get(ListOfPSADetailsId) match {
      case Some(x) => x.find(_.id == psaId).flatMap(_.relationshipDate)
      case None => None
    }
  }

  private def formatRelationshipDate(relationshipDateString: Option[String]): Option[String] = {
    relationshipDateString match {
      case Some(dateString) =>
        val relationshipDate: LocalDate = LocalDate.parse(dateString)
        val formattedDate: String = DateHelper.formatDate(relationshipDate)
        Some(formattedDate)
      case None => None
    }
  }

  private def getFormattedRelationshipDate(ua: UserAnswers, psaId: String): String = {
    val date = getRelationshipDate(ua: UserAnswers, psaId: String)
    formatRelationshipDate(date).getOrElse(throw new RuntimeException("No relationship date found."))
  }

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] =
                (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSANameId and AssociatedDateId).retrieve.map {
        case schemeName ~ psaName ~ associationDate =>
          Future.successful(
            Ok(
              view(
                form(associationDate),
                psaName,
                schemeName,
                srn,
                getFormattedRelationshipDate(request.userAnswers, request.psaId.map(_.value).getOrElse(""))
              )
            )
          )
        case _ =>
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
      }
  }

  def onSubmit(srn: SchemeReferenceNumber): Action[AnyContent] =
              (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSANameId and PSTRId and AssociatedDateId).retrieve.map {
        case schemeName ~ psaName ~ pstr ~ associationDate =>
          form(associationDate).bindFromRequest().fold(
            (formWithErrors: Form[_]) =>
              Future.successful(BadRequest(view(formWithErrors, psaName, schemeName, srn, formatDate(associationDate)))),
            value =>
              dataCacheConnector.save(request.externalId, PsaRemovalDateId, value).flatMap { cacheMap =>
                psaRemovalConnector.remove(PsaToBeRemovedFromScheme(request.psaIdOrException.id, pstr, value)).flatMap { _ =>
                  val updateDataAndlockRemovalResult = lockConnector.getLockByPsa(request.psaIdOrException.id).map {
                    case Some(lockedSchemeVariance) if lockedSchemeVariance.srn == srn.id =>
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
