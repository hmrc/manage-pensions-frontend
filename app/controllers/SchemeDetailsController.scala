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
import connectors.{ListOfSchemesConnector, SchemeDetailsConnector}
import controllers.actions._
import javax.inject.Inject
import models.{ListOfSchemes, PsaDetails, PsaSchemeDetails, SchemeDetail}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.schemeDetails

class SchemeDetailsController @Inject()(appConfig: FrontendAppConfig,
                                          override val messagesApi: MessagesApi,
                                          schemeDetailsConnector: SchemeDetailsConnector,
                                          listSchemesConnector: ListOfSchemesConnector,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction) extends FrontendController with I18nSupport {

  def onPageLoad(srn: String): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

      schemeDetailsConnector.getSchemeDetails("srn", srn).flatMap { scheme =>
        listSchemesConnector.getListOfSchemes(request.psaId.id).map { list =>

          val isSchemeOpen = scheme.psaSchemeDetails.schemeDetails.schemeStatus.equalsIgnoreCase("open")

                Ok(schemeDetails(appConfig,
                  scheme.psaSchemeDetails.schemeDetails.schemeName,
                  openedDate(srn, list, isSchemeOpen),
                  administrators(scheme),
                  srn,
                  isSchemeOpen
                ))
            }
      }
  }

  private def administrators(scheme: PsaSchemeDetails): Option[Seq[String]] = {
    scheme.psaSchemeDetails.psaDetails match {
      case Some(psaDetails) => Some(psaDetails.map(fullName(_)))
      case None => None
    }
  }

  private def openedDate(srn: String, list: ListOfSchemes, isSchemeOpen: Boolean): Option[String] = {
    isSchemeOpen match {
      case true =>
        list.schemeDetail.flatMap { listOfSchemes =>
          val currentScheme = listOfSchemes.filter((i: SchemeDetail) => i.referenceNumber.contains(srn))
          if (currentScheme.nonEmpty) {
            currentScheme.head.openDate.map(new LocalDate(_).toString(formatter))
          } else {
            None
          }
        }
      case _ => None
    }
  }

  private def fullName(psa: PsaDetails): String =
    s"${psa.firstName.getOrElse("")} ${psa.middleName.getOrElse("")} ${psa.lastName.getOrElse("")}"

  private val formatter = DateTimeFormat.forPattern("dd MMMM YYYY")

}
