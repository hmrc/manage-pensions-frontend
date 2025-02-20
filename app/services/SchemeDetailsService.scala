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

package services

import java.time.LocalDate
import com.google.inject.Inject
import connectors.FrontendConnector
import connectors.admin.MinimalConnector
import connectors.scheme.PensionSchemeVarianceLockConnector
import identifiers.SchemeStatusId
import identifiers.psa.ListOfPSADetailsId
import models.SchemeStatus.{WoundUp, Deregistered, Open}
import models._
import models.psa.PsaSchemeDetails
import models.requests.AuthenticatedRequest
import play.api.mvc.{AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import utils.{DateHelper, UserAnswers}
import viewmodels.AssociatedPsa

import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsService @Inject()(frontendConnector: FrontendConnector,
                                     schemeVarianceLockConnector: PensionSchemeVarianceLockConnector,
                                     minimalPsaConnector: MinimalConnector
                                    )(implicit ec: ExecutionContext) {

  def retrieveAftHtml[A](userAnswers: UserAnswers, srn: String)
                        (implicit request: Request[A]): Future[Html] =
    if (isCorrectSchemeStatus(userAnswers)) {
      frontendConnector.retrieveAftPartial(srn)
    } else {
      Future.successful(Html(""))
    }

  def retrievePspSchemeDashboardCards[A](srn: String, pspId: String, authorisingPsaId: String)
                                           (implicit request: Request[A]): Future[Html] =
    frontendConnector.retrievePspSchemeDashboardCards(srn, pspId, authorisingPsaId)

  def retrievePaymentsAndChargesHtml[A](srn: String)
                                       (implicit request: Request[A]): Future[Html] =
    frontendConnector.retrievePaymentsAndChargesPartial(srn)

  private def isCorrectSchemeStatus(ua: UserAnswers): Boolean = {
    val validStatus = Seq(Open.value, WoundUp.value, Deregistered.value)
    ua.get(SchemeStatusId) match {
      case Some(schemeStatus) if validStatus.contains(schemeStatus.capitalize) =>
        true
      case _ =>
        false
    }
  }

  def displayChangeLink(isSchemeOpen: Boolean, lock: Option[Lock]): Boolean = {
    if (!isSchemeOpen) {
      false
    } else {
      lock match {
        case Some(VarianceLock) | None => true
        case Some(_) => false
      }
    }
  }

  def administratorsVariations(psaId: String, psaSchemeDetails: UserAnswers, schemeStatus: String): Option[Seq[AssociatedPsa]] =
    psaSchemeDetails.get(ListOfPSADetailsId).map { psaDetailsSeq =>
      psaDetailsSeq.map { psaDetails =>
        val name = psaDetails.getPsaName.getOrElse("")
        val canRemove = psaDetails.id.equals(psaId) && PsaSchemeDetails.canRemovePsaVariations(psaId, psaDetailsSeq, schemeStatus)
        AssociatedPsa(name, canRemove)
      }
    }

  def openedDate(srn: String, list: ListOfSchemes, isSchemeOpen: Boolean): Option[String] =
    if (isSchemeOpen) {
      list.schemeDetails.flatMap {
        listOfSchemes =>
          val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
          if (currentScheme.nonEmpty) {
            currentScheme.head.openDate.map(date => LocalDate.parse(date).format(DateHelper.formatter))
          } else {
            None
          }
      }
    }
    else {
      None
    }

  def pstr(srn: String, list: ListOfSchemes): Option[String] =
    list.schemeDetails.flatMap { listOfSchemes =>
      val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
      if (currentScheme.nonEmpty) {
        currentScheme.head.pstr
      } else {
        None
      }
    }

  def lockingPsa(lock: Option[Lock], srn: SchemeReferenceNumber)
                (implicit request: AuthenticatedRequest[AnyContent], hc: HeaderCarrier): Future[Option[String]] =
    lock match {
      case Some(SchemeLock) => schemeVarianceLockConnector.getLockByScheme(srn) flatMap {
        case Some(schemeVariance) if !(schemeVariance.psaId == request.psaIdOrException.id) =>
          minimalPsaConnector.getPsaNameFromPsaID().map(identity)
        case _ => Future.successful(None)
      }
      case _ => Future.successful(None)
    }
}
