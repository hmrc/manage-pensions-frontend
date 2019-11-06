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

package services

import config.FrontendAppConfig
import connectors.{DeregistrationConnector, MinimalPsaConnector, PensionSchemeVarianceLockConnector, UpdateSchemeCacheConnector, UserAnswersCacheConnector}
import controllers.actions.{AuthAction, DataRetrievalAction}
import controllers.routes._
import javax.inject.Inject
import models.requests.OptionalDataRequest
import models.{LastUpdatedDate, Link, MinimalPSA}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.libs.json.{JsError, JsResultException, JsSuccess, JsValue}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier
import utils.annotations.PensionsSchemeCache
import viewmodels.{CardViewModel, Message}

import scala.concurrent.{ExecutionContext, Future}

class SchemesOverviewService@Inject()(appConfig: FrontendAppConfig,
                                      override val messagesApi: MessagesApi,
                                      @PensionsSchemeCache dataCacheConnector: UserAnswersCacheConnector,
                                      minimalPsaConnector: MinimalPsaConnector,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      pensionSchemeVarianceLockConnector: PensionSchemeVarianceLockConnector,
                                      updateConnector: UpdateSchemeCacheConnector,
                                      deregistrationConnector: DeregistrationConnector
                                     )(implicit ec: ExecutionContext) extends I18nSupport {


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

  def getTiles(psaId: String)(implicit request: OptionalDataRequest[AnyContent], hc: HeaderCarrier): Future[Seq[CardViewModel]] =
    subscriptionLinks.flatMap { subLinks =>
      variationsLinks(psaId).map { varLinks =>
        Seq(adminCard(psaId), schemeCard(subLinks, varLinks))
      }
    }

  private def adminCard(psaId: String) = CardViewModel(
    id = Some("administrator-card"),
    heading = Message("messages__schemeOverview__psa_heading"),
    subHeading = Some(Message("messages__schemeOverview__psa_id", psaId)),
    links = Seq(
      Link("psaLink", appConfig.registeredPsaDetailsUrl, Message("messages__schemeOverview__psa_change"))
    )
  )

  private def schemeCard(subscriptionLinks: Seq[Link],
                 variationsLinks: Seq[Link] = Nil) = CardViewModel(
    id = Some("scheme-card"),
    heading = Message("messages__schemeOverview__scheme_heading"),
    links = Seq(
      Link("view-schemes", ListSchemesController.onPageLoad().url, Message("messages__schemeOverview__scheme_view"))
    ) ++ subscriptionLinks ++ variationsLinks
  )

  private def subscriptionLinks(implicit request: OptionalDataRequest[AnyContent], hc: HeaderCarrier): Future[Seq[Link]] =
    dataCacheConnector.fetch(request.externalId).flatMap {

      case None => Future.successful(Seq(Link("register-new-scheme", controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url,
        Message("messages__schemeOverview__scheme_subscription"))))


      case Some(data) =>
        schemeName(data) match {
          case Some(schemeName) =>
            lastUpdatedAndDeleteDate(request.externalId)
              .map(date => Seq(
                Link("continue-registration", controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url,
                  Message("messages__schemeOverview__scheme_subscription_continue", schemeName, createFormattedDate(date, appConfig.daysDataSaved))),
                Link("delete-registration", controllers.routes.DeleteSchemeController.onPageLoad().url,
                  Message("messages__schemeOverview__scheme_subscription_delete", schemeName))))
          case _ => Future.successful(Seq.empty[Link])
        }
    }

  private def variationsLinks(psaId: String)(implicit hc: HeaderCarrier): Future[Seq[Link]] =

    pensionSchemeVarianceLockConnector.getLockByPsa(psaId).flatMap {
      case Some(schemeVariance) =>
        updateConnector.fetch(schemeVariance.srn).flatMap {
          case Some(data) => variationsDeleteDate(schemeVariance.srn).map { dateOfDeletion =>
            val schemeName = (data \ "schemeName").as[String]
            Seq(
              Link("continue-variation", appConfig.viewSchemeDetailsUrl.format(schemeVariance.srn),
                Message("messages__schemeOverview__scheme_variations_continue", schemeName, dateOfDeletion)),
              Link("delete-variation", controllers.routes.DeleteSchemeChangesController.onPageLoad(schemeVariance.srn).url,
                Message("messages__schemeOverview__scheme_variations_delete", schemeName)))
          }
          case None => Future.successful(Seq.empty[Link])
        }
      case None =>
        Future.successful(Seq.empty[Link])
    }

  def retrieveResult(schemeDetailsCache: Option[JsValue], psaMinimalDetails: Option[MinimalPSA]
                            )(implicit request: OptionalDataRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {
    schemeDetailsCache match {
      case None => Future.successful(redirectBasedOnPsaSuspension(appConfig.registerSchemeUrl, psaMinimalDetails))
      case Some(schemeDetails) => schemeName(schemeDetails) match {
        case Some(_) => Future.successful(redirectBasedOnPsaSuspension(appConfig.continueSchemeUrl, psaMinimalDetails))
        case _ => deleteDataIfSrnNumberFoundAndRedirect(schemeDetails, psaMinimalDetails)
      }
    }
  }

  private def deleteDataIfSrnNumberFoundAndRedirect(data: JsValue,
                                                    psaMinimalDetails: Option[MinimalPSA]
                                                   )(implicit request: OptionalDataRequest[AnyContent], hc: HeaderCarrier): Future[Result] =
    (data \ "submissionReferenceNumber" \ "schemeReferenceNumber").validate[String].fold({_ =>
      Logger.warn("Page load failed since both scheme name and srn number were not found in scheme registration mongo collection")
      Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))},
      _ => dataCacheConnector.removeAll(request.externalId).map {_ =>
        Logger.warn("Data cleared as scheme name is missing and srn number was found in mongo collection")
        redirectBasedOnPsaSuspension(appConfig.registerSchemeUrl, psaMinimalDetails)
      })

  private def redirectBasedOnPsaSuspension(redirectUrl: String, psaMinimalDetails:  Option[MinimalPSA]): Result =
    psaMinimalDetails.fold(Redirect(redirectUrl)){ psaMinDetails =>
      if (psaMinDetails.isPsaSuspended) {
        Redirect(CannotStartRegistrationController.onPageLoad())
      } else {
        Redirect(redirectUrl)
      }
    }

  private val formatter = DateTimeFormat.forPattern("dd MMMM YYYY")

  private def createFormattedDate(dt: LastUpdatedDate, daysToAdd: Int): String = new LocalDate(dt.timestamp).plusDays(daysToAdd).toString(formatter)

  private def currentTimestamp: LastUpdatedDate = LastUpdatedDate(DateTime.now(DateTimeZone.UTC).getMillis)



}
