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

package controllers

import config.FrontendAppConfig
import connectors.{UserAnswersCacheConnector, MinimalPsaConnector}
import controllers.actions._
import javax.inject.Inject
import models.requests.OptionalDataRequest
import models.{LastUpdatedDate, MinimalPSA}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsResultException, JsSuccess, JsValue}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.PensionsSchemeCache
import views.html.schemesOverview

import scala.concurrent.Future

class SchemesOverviewController @Inject()(appConfig: FrontendAppConfig,
                                          override val messagesApi: MessagesApi,
                                          @PensionsSchemeCache dataCacheConnector: UserAnswersCacheConnector,
                                          minimalPsaConnector: MinimalPsaConnector,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction) extends FrontendController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

      dataCacheConnector.fetch(request.externalId).flatMap {
        case None =>
          if (appConfig.isWorkPackageOneEnabled) {
            minimalPsaConnector.getMinimalPsaDetails(request.psaId.id).map { minimalDetails =>
              Ok(schemesOverview(appConfig, None, None, None, getPsaName(minimalDetails)))
            }
          } else {
            Future.successful(Ok(schemesOverview(appConfig, None, None, None, None)))
          }
        case Some(data) =>
          (data \ "schemeDetails" \ "schemeName").validate[String] match {
            case JsSuccess(name, _) =>
              dataCacheConnector.lastUpdated(request.externalId).flatMap { dateOpt =>
                if (appConfig.isWorkPackageOneEnabled) {
                  minimalPsaConnector.getMinimalPsaDetails(request.psaId.id).map { minimalDetails =>
                    buildView(name, dateOpt, Some(minimalDetails))
                  }
                } else {
                  Future.successful(buildView(name, dateOpt))
                }
              }
            case JsError(_) => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }
      }
  }

  private def buildView(name: String, dateOpt: Option[JsValue],
                        minimalDetails: Option[MinimalPSA] = None)(implicit request: OptionalDataRequest[AnyContent]) = {
    val date = dateOpt.map(ts =>
      LastUpdatedDate(
        ts.validate[Long] match {
          case JsSuccess(value, _) => value
          case JsError(errors) => throw JsResultException(errors)
        }
      )
    ).getOrElse(currentTimestamp)

    Ok(schemesOverview(
      appConfig,
      Some(name),
      Some(s"${createFormattedDate(date, daysToAdd = 0)}"),
      Some(s"${createFormattedDate(date, appConfig.daysDataSaved)}"),
      minimalDetails.flatMap(getPsaName)
    ))
  }

  private def getPsaName(minimalDetails: MinimalPSA): Option[String] = {
    (minimalDetails.individualDetails, minimalDetails.organisationName) match {
      case (Some(individual), None) => Some(individual.fullName)
      case (None, Some(org)) => Some(s"$org")
      case _ => None
    }
  }

  def onClickCheckIfSchemeCanBeRegistered: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      if (appConfig.isWorkPackageOneEnabled) {
        for {
          data <- dataCacheConnector.fetch(request.externalId)
          psaMinimalDetails <- minimalPsaConnector.getMinimalPsaDetails(request.psaId.id)
        } yield {
          retrieveResult(data, Some(psaMinimalDetails))
        }
      } else {
        dataCacheConnector.fetch(request.externalId).map { data =>
          retrieveResult(data, None)
        }
      }
  }

  private def retrieveResult(schemeDetails: Option[JsValue], psaMinimalDetails: Option[MinimalPSA]): Result = {
    schemeDetails match {
      case None => psaMinimalDetails.fold(Redirect(appConfig.registerSchemeUrl))(details => redirect(appConfig.registerSchemeUrl, details))
      case Some(details) => (details \ "schemeDetails" \ "schemeName").validate[String] match {
        case JsSuccess(_, _) => psaMinimalDetails.fold(Redirect(appConfig.continueSchemeUrl))(details => redirect(appConfig.continueSchemeUrl, details))
        case JsError(_) => Redirect(controllers.routes.SessionExpiredController.onPageLoad())
      }
    }
  }

  private def redirect(redirectUrl: String, psaMinimalDetails: MinimalPSA): Result = {
    if (psaMinimalDetails.isPsaSuspended) {
      Redirect(routes.CannotStartRegistrationController.onPageLoad())
    } else {
      Redirect(redirectUrl)
    }
  }

  private val formatter = DateTimeFormat.forPattern("dd MMMM YYYY")

  private def createFormattedDate(dt: LastUpdatedDate, daysToAdd: Int): String = new LocalDate(dt.timestamp).plusDays(daysToAdd).toString(formatter)

  private def currentTimestamp: LastUpdatedDate = LastUpdatedDate(DateTime.now(DateTimeZone.UTC).getMillis)
}
