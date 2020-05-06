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
import java.time.format.DateTimeFormatter
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.admin.MinimalPsaConnector
import connectors.aft.{AFTConnector, AftCacheConnector}
import connectors.scheme.PensionSchemeVarianceLockConnector
import identifiers.{ListOfPSADetailsId, SchemeStatusId}
import identifiers.invitations.PSTRId
import models.SchemeStatus.{Deregistered, Open, WoundUp}
import models._
import models.requests.AuthenticatedRequest
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import utils.{DateHelper, UserAnswers}
import viewmodels.{AFTViewModel, AssociatedPsa, Message}
import scala.concurrent.{ExecutionContext, Future}
import utils.DateHelper._

class SchemeDetailsService @Inject()(appConfig: FrontendAppConfig,
                                     aftConnector: AFTConnector,
                                     aftCacheConnector: AftCacheConnector,
                                     schemeVarianceLockConnector: PensionSchemeVarianceLockConnector,
                                     minimalPsaConnector: MinimalPsaConnector
                                    )(implicit ec: ExecutionContext) {
  def retrieveOptionAFTViewModel(userAnswers: UserAnswers, srn: String)(implicit hc: HeaderCarrier): Future[Seq[AFTViewModel]] = {
    if (appConfig.isAFTEnabled && isCorrectSchemeStatus(userAnswers)) {
      val pstrId = userAnswers.get(PSTRId)
        .getOrElse(throw new RuntimeException(s"No PSTR ID found for srn $srn"))

      if(isOverviewApiDisabled) { //TODO This case to be deleted after 1 July 2020 and only the else section for this if to remain
        for {
          optVersions <- aftConnector.getListOfVersions(pstrId)
          optLockedBy <- aftCacheConnector.lockedBy(srn, appConfig.quarterStartDate)
        } yield {
          createAFTViewModel(optVersions, optLockedBy, srn, appConfig.quarterStartDate, appConfig.quarterEndDate)
        }
      } else {
        createAFTOverviewModel(pstrId, srn)
      }
    } else {
      Future.successful(Nil)
    }
  }

  private def isOverviewApiDisabled: Boolean =
    LocalDate.parse(appConfig.overviewApiEnablementDate).isAfter(DateHelper.currentDate)


  private def createAFTOverviewModel(pstrId: String, srn: String)(
    implicit hc: HeaderCarrier): Future[Seq[AFTViewModel]] = {
    for {
      overview <- aftConnector.getAftOverview(pstrId)
      inProgressReturnsOpt <- getInProgressReturnsModel(overview, srn, pstrId)
      startReturnsOpt <- getStartReturnsModel(overview, srn, pstrId)
    } yield {
      Seq(inProgressReturnsOpt, startReturnsOpt, getPastReturnsModel(overview, srn)).flatten
    }
  }


  /* Returns a start link if:
      1. Return has not been initiated for any of the quarters that are valid for starting a return OR
      2. Any of the returns in their first compile have been zeroed out due to deletion of all charges
   */

  private def getStartReturnsModel(overview: Seq[AFTOverview], srn: String, pstr: String
                                  )(implicit hc: HeaderCarrier): Future[Option[AFTViewModel]] = {

    val startLink: Option[AFTViewModel] = Some(AFTViewModel(None, None,
        Link(id = "aftLoginLink", url = appConfig.aftLoginUrl.format(srn),
          linkText = Message("messages__schemeDetails__aft_start"))))

    val isReturnNotInitiatedForAnyQuarter: Boolean = {
      val aftValidYears = aftConnector.aftStartDate.getYear to aftConnector.aftEndDate.getYear
      aftValidYears.flatMap { year =>
        Quarters.validQuartersForYear(year)(appConfig).map { quarter =>
          !overview.map(_.periodStartDate).contains(Quarters.getQuarterDates(quarter, year).startDate)
        }
      }.contains(true)
    }

    if (isReturnNotInitiatedForAnyQuarter) {
     Future.successful(startLink)
    } else {

      retrieveZeroedOutReturns(overview, pstr).map {
        case zeroedReturns if zeroedReturns.nonEmpty => startLink //if any returns in first compile are zeroed out, display start link
        case _ => None
      }
    }
  }

  /* Returns a seq of the aftReturns in their first compile have been zeroed out due to deletion of all charges
  */
  private def retrieveZeroedOutReturns(overview: Seq[AFTOverview], pstr: String
                                      )(implicit hc: HeaderCarrier): Future[Seq[AFTOverview]] = {
    val firstCompileReturns = overview.filter(_.compiledVersionAvailable).filter(_.numberOfVersions == 1)

      Future.sequence(firstCompileReturns.map(aftReturn =>
        aftConnector.getIsAftNonZero(pstr, aftReturn.periodStartDate.toString, "1"))).map {
        isNonZero => (firstCompileReturns zip isNonZero).filter(!_._2).map(_._1)
      }
  }

  private def getPastReturnsModel(overview: Seq[AFTOverview], srn: String)(implicit hc: HeaderCarrier): Option[AFTViewModel] = {
    val pastReturns = overview.filter(!_.compiledVersionAvailable)

    if (pastReturns.nonEmpty) {
      Some(AFTViewModel(
        None,
        None,
        Link(
          id = "aftAmendLink",
          url = appConfig.aftAmendUrl.format(srn),
          linkText = Message("messages__schemeDetails__aft_view_or_change"))
      ))
    } else {
      None
    }
  }


    private def getInProgressReturnsModel(overview: Seq[AFTOverview], srn: String, pstr: String)(implicit hc: HeaderCarrier): Future[Option[AFTViewModel]] = {
    val inProgressReturns = overview.filter(_.compiledVersionAvailable)

    if(inProgressReturns.size == 1){
      val startDate: LocalDate = inProgressReturns.head.periodStartDate
      val endDate: LocalDate = Quarters.getQuarterDates(startDate).endDate

      if(inProgressReturns.head.numberOfVersions == 1) {
       aftConnector.getIsAftNonZero(pstr, startDate.toString, "1").flatMap {
          case true => modelForSingleInProgressReturn(srn, startDate, endDate, inProgressReturns.head)
          case _ => Future.successful(None)
       }
      } else {
        modelForSingleInProgressReturn(srn, startDate, endDate, inProgressReturns.head)
      }

    } else if(inProgressReturns.nonEmpty) {
      modelForMultipleInProgressReturns(srn, pstr, inProgressReturns)
    }
    else {
      Future.successful(None)
    }
  }

  private def modelForSingleInProgressReturn(srn: String, startDate: LocalDate, endDate: LocalDate, overview: AFTOverview
                                            )(implicit  hc: HeaderCarrier): Future[Option[AFTViewModel]] = {
    aftCacheConnector.lockedBy(srn, startDate.toString).map {
      case Some(lockedBy) => Some(AFTViewModel(
        Some(Message("messages__schemeDetails__aft_period", startDate.format(startDateFormat), endDate.format(endDateFormat))),
        if (lockedBy.nonEmpty) {
          Some(Message("messages__schemeDetails__aft_lockedBy", lockedBy))
        }
        else {
          Some(Message("messages__schemeDetails__aft_locked"))
        },
        if(overview.submittedVersionAvailable) {
          Link(id = "aftReturnHistoryLink", url = appConfig.aftReturnHistoryUrl.format(srn, startDate),
            linkText = Message("messages__schemeDetails__aft_view"))
        }
        else {
          Link(id = "aftSummaryLink", url = appConfig.aftSummaryPageUrl.format(srn, startDate, 1),
            linkText = Message("messages__schemeDetails__aft_view"))
        }
      ))
      case _ => Some(AFTViewModel(
        Some(Message("messages__schemeDetails__aft_period", startDate.format(startDateFormat), endDate.format(endDateFormat))),
        Some(Message("messages__schemeDetails__aft_inProgress")),
        Link(
          id = "aftReturnHistoryLink",
          url = appConfig.aftReturnHistoryUrl.format(srn, startDate),
          linkText = Message("messages__schemeDetails__aft_view"))
      ))
    }
  }

  private def modelForMultipleInProgressReturns(srn: String, pstr: String, inProgressReturns: Seq[AFTOverview]
                                               )(implicit hc: HeaderCarrier): Future[Option[AFTViewModel]] = {

    retrieveZeroedOutReturns(inProgressReturns, pstr).map { zeroedReturns =>

      val countInProgress:Int = inProgressReturns.size - zeroedReturns.size

      if(countInProgress > 0) {
        Some(AFTViewModel(
          Some(Message("messages__schemeDetails__aft_multiple_inProgress")),
          Some(Message("messages__schemeDetails__aft_inProgressCount").withArgs(countInProgress)),
          Link(
            id = "aftContinueInProgressLink",
            url = appConfig.aftContinueReturnUrl.format(srn),
            linkText = Message("messages__schemeDetails__aft_view"))
        ))
    } else {
      None
      }
    }
  }

  private def isCorrectSchemeStatus(ua: UserAnswers): Boolean = {
    val validStatus = Seq(Open.value, WoundUp.value, Deregistered.value)
    ua.get(SchemeStatusId) match {
      case Some(schemeStatus) if validStatus.contains(schemeStatus.capitalize) =>
        true
      case _ =>
        false
    }
  }

  private def createAFTViewModel(optVersions: Option[Seq[AFTVersion]], optLockedBy: Option[String],
                                 srn: String, startDate: String, endDate: String): Seq[AFTViewModel] = {
    val dateFormatterYMD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val formattedStartDate: String = LocalDate.parse(startDate, dateFormatterYMD).format(startDateFormat)
    val formattedEndDate: String = LocalDate.parse(endDate, dateFormatterYMD).format(endDateFormat)
    (optVersions, optLockedBy) match {
      case (Some(versions), None) if versions.isEmpty =>
        Seq(AFTViewModel(None, None,
          Link(id = "aftChargeTypePageLink", url = appConfig.aftLoginUrl.format(srn),
            linkText = Message("messages__schemeDetails__aft_startLink", formattedStartDate, formattedEndDate)))
        )
      case (Some(versions), Some(name)) if versions.isEmpty =>
        Seq(AFTViewModel(
          Some(Message("messages__schemeDetails__aft_period", formattedStartDate, formattedEndDate)),
          if (name.nonEmpty) {
            Some(Message("messages__schemeDetails__aft_lockedBy", name))
          }
          else {
            Some(Message("messages__schemeDetails__aft_locked"))
          },
          Link(id = "aftSummaryPageLink", url = appConfig.aftSummaryPageNoVersionUrl.format(srn, startDate),
            linkText = Message("messages__schemeDetails__aft_view"))
        )
        )
      case (Some(versions), Some(name)) =>
        Seq(AFTViewModel(
          Some(Message("messages__schemeDetails__aft_period", formattedStartDate, formattedEndDate)),
          if (name.nonEmpty) {
            Some(Message("messages__schemeDetails__aft_lockedBy", name))
          }
          else {
            Some(Message("messages__schemeDetails__aft_locked"))
          },
          Link(id = "aftSummaryPageLink", url = appConfig.aftSummaryPageUrl.format(srn, startDate, versions.head.reportVersion),
            linkText = Message("messages__schemeDetails__aft_view"))
        )
        )

      case (Some(versions), None) =>
        Seq(AFTViewModel(
          Some(Message("messages__schemeDetails__aft_period", formattedStartDate, formattedEndDate)),
          Some(Message("messages__schemeDetails__aft_inProgress")),
          Link(
            id = "aftSummaryPageLink",
            url = appConfig.aftSummaryPageUrl.format(srn, startDate, versions.head.reportVersion),
            linkText = Message("messages__schemeDetails__aft_view"))
        )
        )
      case _ => Nil
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
