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

import config.{FeatureSwitchManagementService, FrontendAppConfig}
import connectors.{MinimalPsaConnector, PensionSchemeVarianceLockConnector, UpdateSchemeCacheConnector, UserAnswersCacheConnector}
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Toggles
import utils.annotations.PensionsSchemeCache
import views.html.schemesOverview

import scala.concurrent.{ExecutionContext, Future}

class SchemesOverviewController @Inject()(appConfig: FrontendAppConfig,
                                          override val messagesApi: MessagesApi,
                                          @PensionsSchemeCache dataCacheConnector: UserAnswersCacheConnector,
                                          minimalPsaConnector: MinimalPsaConnector,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          pensionSchemeVarianceLockConnector: PensionSchemeVarianceLockConnector,
                                          updateConnector: UpdateSchemeCacheConnector,
                                          featureSwitchManagementService: FeatureSwitchManagementService
                                         )
                                         (implicit val ec: ExecutionContext) extends FrontendController with I18nSupport {

  def redirect: Action[AnyContent] = Action.async(Future.successful(Redirect(controllers.routes.SchemesOverviewController.onPageLoad())))

  //TODO: Remove this code and just use scheme name after 28 days of enabling hub v2
  private def schemeName(data: JsValue): Option[String] = {
    val schemeName: JsLookupResult = data \ "schemeName"

    val jsLookupResult = schemeName.validate[String] match {
      case JsSuccess(_, _) => schemeName
      case _ => data \ "schemeDetails" \ "schemeName"
    }
    jsLookupResult.validate[String] match {
      case JsSuccess(name, _) => Option(name)
      case JsError(e) =>
        Logger.error(s"Unable to retrieve scheme name from user answers: $e")
        None
    }
  }

  private def lastUpdatedAndDeleteDate(externalId: String)(implicit hc: HeaderCarrier): Future[(Option[String], Option[String])] = {
    dataCacheConnector.lastUpdated(externalId).map { dateOpt =>
      val date = dateOpt.map(ts =>
        LastUpdatedDate(
          ts.validate[Long] match {
            case JsSuccess(value, _) => value
            case JsError(errors) => throw JsResultException(errors)
          }
        )
      ).getOrElse(currentTimestamp)
      (
        Option(s"${createFormattedDate(date, daysToAdd = 0)}"),
        Option(s"${createFormattedDate(date, appConfig.daysDataSaved)}")
      )
    }
  }

  private def variationsDeleteDate(srn: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    updateConnector.lastUpdated(srn).map { dateOpt =>
      val date = dateOpt.map { ts =>
        LastUpdatedDate(
          ts.validate[Long] match {
            case JsSuccess(value, _) => value
            case JsError(errors) => throw JsResultException(errors)
          }
        )
      }.getOrElse(currentTimestamp)
      Option(s"${createFormattedDate(date, appConfig.daysDataSaved)}")
    }
  }

  private def registerSchemeUrl = appConfig.registerSchemeUrl

  private def variationsInfo(psaId: String)(implicit hc: HeaderCarrier): Future[(Option[String], Option[String])] = {
    if (featureSwitchManagementService.get(Toggles.isVariationsEnabled)) {
      pensionSchemeVarianceLockConnector.getLockByPsa(psaId).flatMap { _
          .fold[Future[(Option[String], Option[String])]](Future.successful((None, None))) { schemeVariance =>
          updateConnector.fetch(schemeVariance.srn).flatMap {
            case Some(data) => variationsDeleteDate(schemeVariance.srn).map(((data \ "schemeName").validate[String].asOpt, _))
            case None => Future.successful((None, None))
          }
        }
      }
    } else {
      Future.successful((None,None))
    }
  }

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      val currentRegistrationInfo: Future[Option[(Option[String], Option[String], Option[String])]] =
        dataCacheConnector.fetch(request.externalId).flatMap {
          case None =>
            Future.successful(Some((None, None, None)))
          case Some(data) =>
            schemeName(data) match {
              case schemeName@Some(_) =>
                lastUpdatedAndDeleteDate(request.externalId)
                  .map(dates => Option((schemeName, dates._1, dates._2)))
              case _ => Future.successful(None)
            }
        }

      currentRegistrationInfo.flatMap {
        case Some(crd) =>
          val psaId = request.psaId.id
          variationsInfo(psaId).flatMap { vi =>
            minimalPsaConnector.getPsaNameFromPsaID(psaId).map { psaName =>
              buildView(
                schemeName = crd._1,
                lastDateOpt = crd._2,
                deleteDateOpt = crd._3,
                psaName = psaName,
                psaId = request.psaId.id,
                variationSchemeName = vi._1,
                variationDeleteDate = vi._2
              )
            }
          }
        case None => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  private def buildView(schemeName: Option[String],
                        lastDateOpt: Option[String],
                        deleteDateOpt: Option[String],
                        psaName: Option[String] = None,
                        psaId: String,
                        variationSchemeName: Option[String],
                        variationDeleteDate: Option[String]
                       )(implicit request: OptionalDataRequest[AnyContent]) = {


    Ok(schemesOverview(
      appConfig = appConfig,
      schemeName = schemeName,
      lastDate = lastDateOpt,
      deleteDate = deleteDateOpt,
      name = psaName,
      psaId = psaId,
      variationSchemeName = variationSchemeName,
      variationDeleteDate = variationDeleteDate
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
      case Some(details) => schemeName(details) match {
        case Some(_) => psaMinimalDetails.fold(Redirect(appConfig.continueSchemeUrl))(details => redirect(appConfig.continueSchemeUrl, details))
        case _ =>
          Redirect(controllers.routes.SessionExpiredController.onPageLoad())
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
