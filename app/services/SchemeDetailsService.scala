/*
 * Copyright 2020 HM Revenue & Customs
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
import config.FrontendAppConfig
import connectors.admin.MinimalPsaConnector
import connectors.aft.{AFTConnector, AftCacheConnector}
import connectors.scheme.PensionSchemeVarianceLockConnector
import identifiers.ListOfPSADetailsId
import identifiers.invitations.PSTRId
import models.{Link, ListOfSchemes, Lock, PsaDetails, PsaSchemeDetails, SchemeLock, SchemeReferenceNumber}
import models.requests.AuthenticatedRequest
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import utils.{DateHelper, UserAnswers}
import viewmodels.{AFTViewModel, AssociatedPsa, Message}

import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsService @Inject()(appConfig: FrontendAppConfig,
                                      aftConnector: AFTConnector,
                                      aftCacheConnector: AftCacheConnector,
                                      schemeVarianceLockConnector: PensionSchemeVarianceLockConnector,
                                      minimalPsaConnector: MinimalPsaConnector
                                     )(implicit ec: ExecutionContext){

  def retrieveOptionAFTViewModel(userAnswers: UserAnswers, srn: String)(implicit hc: HeaderCarrier): Future[Option[AFTViewModel]] = {
    if (appConfig.isAFTEnabled) {
      val pstrId = userAnswers.get(PSTRId)
        .getOrElse(throw new RuntimeException(s"No PSTR ID found for srn $srn"))
      for {
        optVersions <- aftConnector.getListOfVersions(pstrId)
        optLockedBy <- aftCacheConnector.lockedBy(srn, appConfig.quarterStartDate)
      } yield {
        (optVersions, optLockedBy) match {
          case (Some(versions), Some(name)) =>
            Option(
              AFTViewModel(
                Some(Message("messages__schemeDetails__aft_period")),
                if(name.nonEmpty)
                  Some(Message("messages__schemeDetails__aft_lockedBy", name))
                else
                  Some(Message("messages__schemeDetails__aft_locked")),
                Link(
                  id = "aftSummaryPageLink",
                  url = appConfig.aftSummaryPageUrl.format(srn, versions.headOption.getOrElse("1")),
                  linkText = Message("messages__schemeDetails__aft_view"))
              )
            )
          case (Some(versions), None) if versions.isEmpty =>
            Option(
              AFTViewModel(
                None,
                None,
                Link(
                  id = "aftChargeTypePageLink",
                  url = appConfig.aftChargeTypePageUrl.format(srn),
                  linkText = Message("messages__schemeDetails__aft_startLink"))
              )
            )

          case (Some(versions), None) =>
            Option(
              AFTViewModel(
                Some(Message("messages__schemeDetails__aft_period")),
                Some(Message("messages__schemeDetails__aft_inProgress")),
                Link(
                  id = "aftSummaryPageLink",
                  url = appConfig.aftSummaryPageUrl.format(srn, versions.headOption.getOrElse("1")),
                  linkText = Message("messages__schemeDetails__aft_view"))
              )
            )

          case _ => None
        }
      }
    } else {
      Future.successful(None)
    }
  }

  def administratorsVariations(psaId: String, psaSchemeDetails: UserAnswers, schemeStatus: String): Option[Seq[AssociatedPsa]] =
    psaSchemeDetails.get(ListOfPSADetailsId).map { psaDetailsSeq =>
      psaDetailsSeq.map { psaDetails =>
        val name = PsaDetails.getPsaName(psaDetails).getOrElse("")
        val canRemove = psaDetails.id.equals(psaId) && PsaSchemeDetails.canRemovePsaVariations(psaId, psaDetailsSeq, schemeStatus)
        AssociatedPsa(name, canRemove)
      }
    }

  def openedDate(srn: String, list: ListOfSchemes, isSchemeOpen: Boolean): Option[String] = {
    if (isSchemeOpen) {
      list.schemeDetail.flatMap {
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
  }

  def pstr(srn: String, list: ListOfSchemes): Option[String] =
    list.schemeDetail.flatMap { listOfSchemes =>
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
        case Some(schemeVariance) if !(schemeVariance.psaId == request.psaId.id) =>
          minimalPsaConnector.getPsaNameFromPsaID(schemeVariance.psaId).map(identity)
        case _ => Future.successful(None)
      }
      case _ => Future.successful(None)
    }
}
