/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.er.EventReportingBackendConnector
import connectors.scheme.{ListOfSchemesConnector, SchemeDetailsConnector}
import controllers.actions.AuthAction
import identifiers.SchemeNameId
import models.{RequiredEventReportingJourneyData, SchemeReferenceNumber}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.net.URLDecoder
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EventReportingNewJourneyController @Inject()(authenticate: AuthAction,
                                                   eventReportingConnector: EventReportingBackendConnector,
                                                   listOfSchemesConnector: ListOfSchemesConnector,
                                                   schemeDetailsConnector: SchemeDetailsConnector,
                                                   appConfig: FrontendAppConfig,
                                                   val controllerComponents: MessagesControllerComponents)
                                                  (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def create(srn: SchemeReferenceNumber, continueUrl:String): Action[AnyContent] = authenticate().async { implicit request =>

    (for {
      schemeName <- schemeDetailsConnector.getSchemeDetails(request.psaIdOrException.id, srn, "srn")
        .map(schemeDetails => schemeDetails.get(SchemeNameId).getOrElse(""))
      listOfSchemesEither <- listOfSchemesConnector.getListOfSchemes(request.psaIdOrException.id)
    } yield {
      val pstr = listOfSchemesEither.toOption.flatMap { listOfSchemes =>
        listOfSchemes.schemeDetails.flatMap (_.find (_.referenceNumber.contains (srn) ) ).flatMap (_.pstr)
      }.getOrElse(throw new RuntimeException("PSTR for scheme not available"))

      eventReportingConnector.createNewJourney(RequiredEventReportingJourneyData(
        pstr,
        schemeName,
        (request.psaId, request.pspId) match {
          case (Some(_), _) => appConfig.psaSchemeDashboardUrl.format(srn.id)
          case (_, Some(_)) => appConfig.pspSchemeDashboardUrl.format(srn.id)
          case _ => throw new RuntimeException("Neither psa or psp id is available")
        }
      )).map { response: HttpResponse =>
        response.status match {
          case OK =>
            val journeyId = (response.json \ "journeyId").as[String]
            val journeyIdParam = if (continueUrl.contains("?")) {
              s"&journeyId=$journeyId"
            } else {
              s"?journeyId=$journeyId"
            }
            Redirect(URLDecoder.decode(continueUrl + journeyIdParam, "UTF-8"))
          case _ => throw new RuntimeException(s"""Backend responded with incorrect status of ${response.status} with body: ${response.body}""")
        }
      }
    }).flatten
  }
}
