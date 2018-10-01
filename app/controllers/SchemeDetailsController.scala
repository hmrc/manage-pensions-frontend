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
import connectors._
import controllers.actions._
import identifiers.SchemeDetailId
import javax.inject.Inject
import models._
import org.joda.time.LocalDate
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.DateHelper
import views.html.schemeDetails

class SchemeDetailsController @Inject()(appConfig: FrontendAppConfig,
                                        override val messagesApi: MessagesApi,
                                        schemeDetailsConnector: SchemeDetailsConnector,
                                        listSchemesConnector: ListOfSchemesConnector,
                                        minimalPsaConnector: MinimalPsaConnector,
                                        userAnswersCacheConnector: UserAnswersCacheConnector,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction) extends FrontendController with I18nSupport {

  def onPageLoad(srn: String): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      for {
        scheme <- schemeDetailsConnector.getSchemeDetails("srn", srn)
        listOfSchemes <- listSchemesConnector.getListOfSchemes(request.psaId.id)
        _ <- userAnswersCacheConnector.save(request.externalId, SchemeDetailId, minimalDetails(scheme))
      } yield {
        val details = scheme.schemeDetails
        val isSchemeOpen = details.status.equalsIgnoreCase("open")

        Ok(schemeDetails(appConfig,
          details.name,
          openedDate(srn, listOfSchemes, isSchemeOpen),
          administrators(scheme),
          srn,
          isSchemeOpen
        ))
      }
  }

  private def minimalDetails(scheme: PsaSchemeDetails): MinimalSchemeDetail = {
    val schemeDetails = scheme.schemeDetails
    MinimalSchemeDetail(schemeDetails.srn.getOrElse(""), schemeDetails.pstr, schemeDetails.name)
  }

  private def administrators(psaSchemeDetails: PsaSchemeDetails): Option[Seq[String]] = {
    psaSchemeDetails.psaDetails.map { psaDetails =>
      psaDetails.map { details =>
        details.individual.map { individual =>
          fullName(individual)
        }.getOrElse("")
      }
    }
  }

  private def openedDate(srn: String, list: ListOfSchemes, isSchemeOpen: Boolean): Option[String] = {
    if (isSchemeOpen) {
      list.schemeDetail.flatMap { listOfSchemes =>
        val currentScheme = listOfSchemes.filter((i: SchemeDetail) => i.referenceNumber.contains(srn))
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

  private def fullName(individual: Name): String =
    s"${individual.firstName.getOrElse("")} ${individual.middleName.getOrElse("")} ${individual.lastName.getOrElse("")}"

  def onClickCheckIfPsaCanInvite: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      minimalPsaConnector.getMinimalPsaDetails(request.psaId.id).map { minimalDetails =>
        if (minimalDetails.isPsaSuspended) {
          Redirect(controllers.invitations.routes.YouCannotSendAnInviteController.onPageLoad())
        } else {
          Redirect(controllers.invitations.routes.PsaNameController.onPageLoad(NormalMode))
        }
      }
  }

}
