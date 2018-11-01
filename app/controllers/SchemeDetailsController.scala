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
import connectors.{ListOfSchemesConnector, SchemeDetailsConnector, UserAnswersCacheConnector}
import controllers.actions._
import javax.inject.Inject
import models._
import org.joda.time.LocalDate
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.DateHelper
import views.html.schemeDetails

import scala.concurrent.Future

class SchemeDetailsController @Inject()(appConfig: FrontendAppConfig,
                                        override val messagesApi: MessagesApi,
                                        schemeDetailsConnector: SchemeDetailsConnector,
                                        listSchemesConnector: ListOfSchemesConnector,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        userAnswersCacheConnector: UserAnswersCacheConnector
                                       ) extends FrontendController with I18nSupport {

  def onPageLoad(srn: String): Action[AnyContent] = authenticate.async {
    implicit request =>
      schemeDetailsConnector.getSchemeDetails("srn", srn).flatMap { scheme =>
        if (scheme.psaDetails.toSeq.flatten.exists(_.id == request.psaId.id)) {
          listSchemesConnector.getListOfSchemes(request.psaId.id).flatMap { list =>
            val schemeDetail = scheme.schemeDetails
            val isSchemeOpen = schemeDetail.status.equalsIgnoreCase("open")

            Future.successful(Ok(schemeDetails(appConfig,
              schemeDetail.name,
              openedDate(srn, list, isSchemeOpen),
              administrators(scheme),
              srn,
              isSchemeOpen
            )))
          }
        } else {
          Future.successful(NotFound)
        }
      }

  }

  private def administrators(psaSchemeDetails: PsaSchemeDetails): Option[Seq[String]] =
    psaSchemeDetails.psaDetails.map(_.flatMap(PsaDetails.getPsaName))

  private def openedDate(srn: String, list: ListOfSchemes, isSchemeOpen: Boolean): Option[String] = {
    if (isSchemeOpen) {
      list.schemeDetail.flatMap { listOfSchemes =>
        val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
        if (currentScheme.nonEmpty) {
          currentScheme.head.openDate.map(new LocalDate(_).toString(DateHelper.formatter))
        } else {
          None
        }
      }
    }
    else {
      None
    }
  }
}
