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
import models.requests.{DataRequest, OptionalDataRequest}
import models.{LastUpdatedDate, MinimalPSA, RegistrationDetails, VariationDetails}
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
                                          updateConnector: UpdateSchemeCacheConnector
                                         )
                                         (implicit val ec: ExecutionContext) extends FrontendController with I18nSupport {

  def redirect: Action[AnyContent] = Action.async(Future.successful(Redirect(controllers.routes.SchemesOverviewController.onPageLoad())))

  private def schemeName(data: JsValue): Option[String] =
    (data \ "schemeName").validate[String].fold(_ => None, Some(_))

  private def parseDateElseCurrent(dateOpt: Option[JsValue]): LastUpdatedDate = {
    dateOpt.map(ts =>
      LastUpdatedDate(
        ts.validate[Long] match {
          case JsSuccess(value, _) => value
          case JsError(errors) => throw JsResultException(errors)
        }
      )
    ).getOrElse(currentTimestamp)
  }

  private def lastUpdatedAndDeleteDate(externalId: String)(implicit hc: HeaderCarrier): Future[LastUpdatedDate] =
    dataCacheConnector.lastUpdated(externalId).map { dateOpt =>
      parseDateElseCurrent(dateOpt)
    }

  private def variationsDeleteDate(srn: String)(implicit hc: HeaderCarrier): Future[String] =
    updateConnector.lastUpdated(srn).map { dateOpt =>
      s"${createFormattedDate(parseDateElseCurrent(dateOpt), appConfig.daysDataSaved)}"
    }

  private def registerSchemeUrl = appConfig.registerSchemeUrl

  private def variationsInfo(psaId: String)(implicit hc: HeaderCarrier): Future[Option[VariationDetails]] =

      pensionSchemeVarianceLockConnector.getLockByPsa(psaId).flatMap {
          case Some(schemeVariance) =>
            updateConnector.fetch(schemeVariance.srn).flatMap {
              case Some(data) => variationsDeleteDate(schemeVariance.srn).map(date =>
                Some(VariationDetails((data \ "schemeName").as[String], date, schemeVariance.srn)))
              case None => Future.successful(None)
            }
          case None =>
            Future.successful(None)
        }

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      val currentRegistrationInfo: Future[Option[RegistrationDetails]] =
        dataCacheConnector.fetch(request.externalId).flatMap {

          case None => Future.successful(None)
          case Some(data) =>
            schemeName(data) match {
              case Some(schemeName) =>
                lastUpdatedAndDeleteDate(request.externalId)
                  .map(date => Some(
                    RegistrationDetails(
                      schemeName,
                      createFormattedDate(date, appConfig.daysDataSaved),
                      createFormattedDate(date, daysToAdd = 0))))
              case _ => Future.successful(None)
            }
        }

      currentRegistrationInfo.flatMap {
        crd =>
          val psaId = request.psaId.id
          variationsInfo(psaId).flatMap { variationDetails =>
            minimalPsaConnector.getPsaNameFromPsaID(psaId).map { psaName =>

              Ok(schemesOverview(
                appConfig,
                crd,
                psaName,
                request.psaId.id,
                variationDetails))
            }
          }

      }
  }

  def onClickCheckIfSchemeCanBeRegistered: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      for {
        data <- dataCacheConnector.fetch(request.externalId)
        psaMinimalDetails <- minimalPsaConnector.getMinimalPsaDetails(request.psaId.id)
        result <- retrieveResult(data, Some(psaMinimalDetails))
      } yield {
        result
      }
  }

  private def retrieveResult(schemeDetailsCache: Option[JsValue], psaMinimalDetails: Option[MinimalPSA]
                            )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    schemeDetailsCache match {
      case None => Future.successful(redirectBasedOnPsaSuspension(registerSchemeUrl, psaMinimalDetails))
      case Some(schemeDetails) => schemeName(schemeDetails) match {
        case Some(_) => Future.successful(redirectBasedOnPsaSuspension(appConfig.continueSchemeUrl, psaMinimalDetails))
        case _ => deleteDataIfSrnNumberFoundAndRedirect(schemeDetails, psaMinimalDetails)
      }
    }
  }

  private def deleteDataIfSrnNumberFoundAndRedirect(data: JsValue,
                                                    psaMinimalDetails: Option[MinimalPSA]
                                                   )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] =
    (data \ "submissionReferenceNumber").validate[String].fold({_ =>
      Logger.warn("Page load failed because both scheme name and srn number were not found in scheme registration mongo collection")
      Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))},
      _ => dataCacheConnector.removeAll(request.externalId).map {_ =>
        redirectBasedOnPsaSuspension(registerSchemeUrl, psaMinimalDetails)
      })

  private def redirectBasedOnPsaSuspension(redirectUrl: String, psaMinimalDetails:  Option[MinimalPSA]): Result =
  psaMinimalDetails.fold(Redirect(redirectUrl)){ psaMinDetails =>
    if (psaMinDetails.isPsaSuspended) {
      Redirect(routes.CannotStartRegistrationController.onPageLoad())
    } else {
      Redirect(redirectUrl)
    }
  }

  private val formatter = DateTimeFormat.forPattern("dd MMMM YYYY")

  private def createFormattedDate(dt: LastUpdatedDate, daysToAdd: Int): String = new LocalDate(dt.timestamp).plusDays(daysToAdd).toString(formatter)

  private def currentTimestamp: LastUpdatedDate = LastUpdatedDate(DateTime.now(DateTimeZone.UTC).getMillis)
}
