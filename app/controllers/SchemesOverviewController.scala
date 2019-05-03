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
import javax.inject.Inject
import models.requests.OptionalDataRequest
import models.{LastUpdatedDate, MinimalPSA}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.PensionsSchemeCache
import views.html.schemesOverview

import scala.concurrent.{ExecutionContext, Future}

class SchemesOverviewController @Inject()(appConfig: FrontendAppConfig,
                                          override val messagesApi: MessagesApi,
                                          @PensionsSchemeCache dataCacheConnector: UserAnswersCacheConnector,
                                          minimalPsaConnector: MinimalPsaConnector,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction)
                                         (implicit val ec: ExecutionContext) extends FrontendController with I18nSupport {

  def redirect: Action[AnyContent] = Action.async(Future.successful(Redirect(controllers.routes.SchemesOverviewController.onPageLoad())))

  //TODO: Remove this code and just use scheme name after 28 days of enabling hub v2
  private def schemeName(data: JsValue): JsLookupResult = {
    val schemeName: JsLookupResult = data \ "schemeName"

    schemeName.validate[String] match {
      case JsSuccess(_, _) => schemeName
      case _ => data \ "schemeDetails" \ "schemeName"
    }
  }

  private def registerSchemeUrl = appConfig.registerSchemeUrl

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

      dataCacheConnector.fetch(request.externalId).flatMap {
        case None => minimalPsaConnector.getPsaNameFromPsaID(request.psaId.id).map { psaName =>
          Ok(schemesOverview(appConfig, None, None, None, psaName, request.psaId.id))
        }
        case Some(data) =>
          schemeName(data).validate[String] match {
            case JsSuccess(name, _) =>
              dataCacheConnector.lastUpdated(request.externalId).flatMap { dateOpt =>
                minimalPsaConnector.getPsaNameFromPsaID(request.psaId.id).map { psaName =>
                  buildView(name, dateOpt, psaName, request.psaId.id)
                }
              }
            case JsError(e) =>{
              Logger.error(s"Unable to retrieve scheme name from user answers: $e")
              Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
            }
          }
      }
  }

  private def buildView(name: String, dateOpt: Option[JsValue],
                        psaName: Option[String] = None, psaId: String)(implicit request: OptionalDataRequest[AnyContent]) = {
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
      psaName,
      psaId
    ))
  }

  def onClickCheckIfSchemeCanBeRegistered: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      for {
        data <- dataCacheConnector.fetch(request.externalId)
        psaMinimalDetails <- minimalPsaConnector.getMinimalPsaDetails(request.psaId.id)
      } yield {
        retrieveResult(data, Some(psaMinimalDetails))
      }
  }

  private def retrieveResult(schemeDetails: Option[JsValue], psaMinimalDetails: Option[MinimalPSA]): Result = {
    schemeDetails match {
      case None => psaMinimalDetails.fold(Redirect(registerSchemeUrl))(details => redirect(registerSchemeUrl, details))
      case Some(details) => schemeName(details).validate[String] match {
        case JsSuccess(_, _) => psaMinimalDetails.fold(Redirect(appConfig.continueSchemeUrl))(details => redirect(appConfig.continueSchemeUrl, details))
        case JsError(e) => {
          Logger.error(s"Unable to retrieve scheme name from user answers: $e")
          Redirect(controllers.routes.SessionExpiredController.onPageLoad())
        }
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
