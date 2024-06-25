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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.PensionSchemeReturnConnector
import connectors.scheme.{PensionSchemeVarianceLockConnector, SchemeDetailsConnector}
import controllers.invitations.psp.routes._
import controllers.invitations.routes._
import controllers.psa.routes._
import controllers.psp.routes._
import identifiers.psa.ListOfPSADetailsId
import identifiers.{SchemeNameId, SchemeStatusId, SeqAuthorisedPractitionerId}
import models.SchemeStatus.Open
import models._
import models.psa.PsaDetails
import models.requests.AuthenticatedRequest
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, RequestHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.DateHelper._
import utils.UserAnswers
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}
import play.twirl.api.Html
import services.PsaSchemeDashboardService.{maxEndDateAsString, minStartDateAsString}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.Try

class PsaSchemeDashboardService @Inject()(
                                           appConfig: FrontendAppConfig,
                                           lockConnector: PensionSchemeVarianceLockConnector,
                                           schemeDetailsConnector: SchemeDetailsConnector,
                                           pensionSchemeReturnConnector: PensionSchemeReturnConnector
                                         )(implicit val ec: ExecutionContext) {

  private val logger = Logger(classOf[PsaSchemeDashboardService])

  private implicit def hc(implicit request: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)

  def optionLockedSchemeName(lock: Option[Lock])(implicit request: AuthenticatedRequest[AnyContent]): Future[Option[String]] = lock match {
    case Some(PsaLock) =>
      val psaId = request.psaIdOrException.id
      lockConnector.getLockByPsa(psaId)(hc(request), implicitly).flatMap { lockedSchemeVariance =>
        lockedSchemeVariance.map(_.srn) match {
          case Some(lockedSrn) =>
            schemeDetailsConnector.getSchemeDetails(psaId, lockedSrn, "srn").map { ua =>
              ua.get(SchemeNameId) match {
                case sn@Some(_) => sn
                case _ => logger.info(
                  s"PSA $psaId has a lock on a scheme. Scheme lock info: $lockedSchemeVariance but " +
                    s"no scheme name found for $lockedSrn")
                  None
              }
            }
          case None =>
            logger.info(s"PSA $psaId has a lock on a scheme. Scheme lock info: $lockedSchemeVariance but no SRN present")
            Future.successful(None)
        }
      }
    case _ => Future.successful(None)
  }

  def cards(
             interimDashboard: Boolean,
             erHtml: Html,
             srn: String,
             lock: Option[Lock],
             list: ListOfSchemes,
             ua: UserAnswers
           )(implicit messages: Messages, request: AuthenticatedRequest[AnyContent]): Future[Seq[CardViewModel]] = {

    val currentScheme: Option[SchemeDetails] = getSchemeDetailsFromListOfSchemes(srn, list)

    val pstr = currentScheme match {
      case Some(cs) if cs.referenceNumber.contains(srn) => cs.pstr.getOrElse("")
      case None => "Pstr Not Found"
    }

    val seqErOverviewFuture: Future[String] = getErOverViewAsString(pstr, interimDashboard)

    val optionLockedSchemeNameFuture = optionLockedSchemeName(lock)

    for {
      seqErOverview <- seqErOverviewFuture
      optionLockedSchemeName <- optionLockedSchemeNameFuture
    } yield {
      if (interimDashboard) {
        Seq(manageReportsEventsCard(srn, erHtml, seqErOverview)) ++ Seq(psaCardForInterimDashboard(srn, ua))
      } else {
        Seq(schemeCard(srn, currentScheme, lock, ua, optionLockedSchemeName)) ++ Seq(psaCard(srn, ua)) ++ pspCard(ua, currentScheme.map(_.schemeStatus))
      }
    }
  }

  private def getErOverViewAsString(pstr: String,
                                    interimDashboard: Boolean)(implicit hc: HeaderCarrier, messages: Messages): Future[String] = {
    if(interimDashboard) {
      pensionSchemeReturnConnector.getOverview(
        pstr, "PSR", minStartDateAsString, maxEndDateAsString
      ).map {
        case x if x.size == 1 =>
          x.head.psrDueDate.map(date => messages("messages__manage_reports_and_returns_psr_due", date.format(formatter)))
            .getOrElse("")
        case x if x.size > 1 =>
          x.head.psrDueDate.map(_ => messages("messages__manage_reports_and_returns_multiple_due"))
            .getOrElse("")
        case _ => ""
      }
    }else {
      Future.successful("")
    }
  }

  //Scheme details card
  private[services] def schemeCard(srn: String,
                                   currentScheme: Option[SchemeDetails],
                                   lock: Option[Lock],
                                   ua: UserAnswers,
                                   lockedSchemeName: Option[String])
                                  (implicit messages: Messages): CardViewModel = {
    CardViewModel(
      id = "scheme_details",
      heading = Message("messages__psaSchemeDash__scheme_details_head"),
      subHeadings = optToSeq(pstrSubHead(currentScheme)) ++ optToSeq(dateSubHead(currentScheme, ua)),
      links = Seq(schemeDetailsLink(srn, ua, lock, currentScheme.map(_.name), lockedSchemeName))
    )
  }

  private[services] def manageReportsEventsCard(srn: String, erHtml:Html, seqEROverview: String)
                               (implicit messages: Messages): CardViewModel = {
    val aftLink = Seq(Link(
        id = "aft-view-link",
        url = appConfig.aftOverviewHtmlUrl.format(srn),
        linkText = messages("messages__aft__view_details_link")
      ))

    val erLink = if (erHtml.equals(Html(""))) {
      Seq()
    } else {
      Seq(Link(
        id = "er-view-link",
        url = appConfig.eventReportingOverviewHtmlUrl.format(srn),
        linkText = messages("messages__er__view_details_link")
      ))
    }

    val psrLink = Seq(
      Link(
        id = "psr-view-details",
        url = appConfig.psrOverviewUrl.format(srn),
        linkText = messages("messages__psr__view_details_link")
      ))


    val subHeading: Seq[CardSubHeading] = if(seqEROverview.isBlank){
      Seq.empty
    }
    else{
      Seq(CardSubHeading(
        subHeading = Message("messages__manage_reports_and_returns_subhead"),
        subHeadingClasses = "card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = seqEROverview,
          subHeadingParamClasses = "font-small bold"))))
    }
    CardViewModel(
      id = "manage_reports_returns",
      heading = Message("messages__manage_reports_and_returns_head"),
      subHeadings =    subHeading,
      links =  aftLink ++ erLink ++ psrLink
    )
  }

  private def optionNotificationMessageKey(optionLock: Option[Lock], lockedSchemeName: Option[String]): Option[String] =
    (optionLock, lockedSchemeName) match {
      case (Some(SchemeLock) | Some(BothLock), _) => Some("messages__psaSchemeDash__view_change_details_link_notification_scheme")
      case (Some(PsaLock), Some(_)) => Some("messages__psaSchemeDash__view_change_details_link_notification_psa")
      case (Some(PsaLock), None) => Some("messages__psaSchemeDash__view_change_details_link_notification_psa-unknown_scheme")
      case _ => None
    }

  def schemeDetailsLink(srn: String,
                        ua: UserAnswers,
                        optionLock: Option[Lock],
                        optionSchemeName: Option[String],
                        lockedSchemeName: Option[String])
                       (implicit messages: Messages): Link = {
    val viewOrChangeLinkText = messages("messages__psaSchemeDash__view_change_details_link")
    val viewLinkText = messages("messages__psaSchemeDash__view_details_link")

    val notification: Option[Message] =
      (optionLock, optionNotificationMessageKey(optionLock, lockedSchemeName), optionSchemeName, lockedSchemeName) match {
        case (Some(PsaLock), Some(key), _, Some(sn)) => Some(Message(key, "<strong>" + sn + "</strong>"))
        case (Some(PsaLock), Some(key), _, None) => Some(Message(key))
        case (_, Some(key), Some(schemeName), _) => Some(Message(key, "<strong>" + schemeName + "</strong>"))
        case _ => None
      }
    val linkText = if (!isSchemeOpen(ua)) {
      viewLinkText
    } else {

      logger.info(s"Pension-scheme : $srn -- Lock-Status : ${optionLock.getOrElse("No-Lock-Found").toString}")

      optionLock match {
        case Some(VarianceLock) | None => viewOrChangeLinkText
        case Some(_) => viewLinkText
      }
    }
    Link(
      id = "view-details",
      url = appConfig.viewSchemeDetailsUrl.format(srn),
      linkText = linkText,
      notification = notification
    )
  }

  private def getSchemeDetailsFromListOfSchemes(srn: String, list: ListOfSchemes): Option[SchemeDetails] =
    list.schemeDetails.flatMap(_.find(_.referenceNumber.contains(srn)))

  private def dateSubHead(currentScheme: Option[SchemeDetails], ua: UserAnswers)
                         (implicit messages: Messages): Option[CardSubHeading] =
    if (isSchemeOpen(ua)) {
      def date: Option[String] = currentScheme.flatMap(_.openDate.map(LocalDate.parse(_).format(formatter)))

      if (currentScheme.nonEmpty && date.nonEmpty) {
        Some(CardSubHeading(
          subHeading = Message("messages__psaSchemeDash__regDate"),
          subHeadingClasses = "card-sub-heading",
          subHeadingParams = Seq(CardSubHeadingParam(
            subHeadingParam = date.getOrElse(""),
            subHeadingParamClasses = "font-small bold"))))
      } else {
        None
      }
    }
    else {
      None
    }

  def psaCardForInterimDashboard(srn: String, ua: UserAnswers)
                                (implicit messages: Messages): CardViewModel =
    CardViewModel(
      id = "psa_psp_list",
      heading = Message("messages__psaSchemeDash__psa_psp_list_head"),
      subHeadings = latestMergedPsaSubHeading(ua) ++ latestPsaSubHeadingDate(ua),
      links = invitePsaLink(isSchemeOpen(ua), srn) ++ Seq(
        Link(
          id = "view-psa-list",
          url = ViewAdministratorsController.onPageLoad(srn).url,
          linkText = Message("messages__psaSchemeDash__view_psa")
        ),
        Link(
          id = "authorise",
          url = WhatYouWillNeedController.onPageLoad().url,
          linkText = Message("messages__pspAuthorise__link")
        ),
        Link(
          id = "view-practitioners",
          url = ViewPractitionersController.onPageLoad().url,
          linkText = Message("messages__pspViewOrDeauthorise__link")
        )
      )
    )

  private def latestMergedPsaSubHeading(ua: UserAnswers)(implicit messages: Messages): Seq[CardSubHeading] =
    latestPsa(ua).fold[Seq[CardSubHeading]](Nil) { psa =>
      Seq(CardSubHeading(
        subHeading = psa.relationshipDate.fold(messages("messages__psaSchemeDash__registered_by"))(date =>
          messages("messages__psaSchemeDash__registered_by", LocalDate.parse(date).format(formatter))),
        subHeadingClasses = "card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = psa.getPsaName.getOrElse(throw PsaNameCannotBeRetrievedException),
          subHeadingParamClasses = "font-small bold"))))
    }


  private def latestPsaSubHeadingDate(ua: UserAnswers)(implicit messages: Messages): Seq[CardSubHeading] =
    latestPsa(ua).fold[Seq[CardSubHeading]](Nil) { psa =>
      psa.relationshipDate.fold[Seq[CardSubHeading]](Nil) { date =>
        Seq(CardSubHeading(
          subHeading = messages("messages__psaSchemeDash__addedOn_date", LocalDate.parse(date).format(formatter)),
          subHeadingClasses = "card-sub-heading",
          subHeadingParams = Seq.empty[CardSubHeadingParam]))
      }
    }

  private def pstrSubHead(currentScheme: Option[SchemeDetails])(implicit messages: Messages): Option[CardSubHeading] = {
    if (currentScheme.exists(_.pstr.nonEmpty)) {
      Some(CardSubHeading(
        subHeading = Message("messages__psaSchemeDash__pstr"),
        subHeadingClasses = "card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = currentScheme.head.pstr.head,
          subHeadingParamClasses = "font-small bold"))))
    } else {
      None
    }
  }

  //PSA card
  def psaCard(srn: String, ua: UserAnswers)
             (implicit messages: Messages): CardViewModel =
    CardViewModel(
      id = "psa_list",
      heading = Message("messages__psaSchemeDash__psa_list_head"),
      subHeadings = latestPsaSubHeading(ua),
      links = invitePsaLink(isSchemeOpen(ua), srn) ++ Seq(
        Link(
          id = "view-psa-list",
          url = ViewAdministratorsController.onPageLoad(srn).url,
          linkText = Message("messages__psaSchemeDash__view_psa")
        )
      )
    )

  private def invitePsaLink(isSchemeOpen: Boolean, srn: String): Seq[Link] =
    if (isSchemeOpen) {
      Seq(Link(
        id = "invite",
        url = InviteController.onPageLoad(srn).url,
        linkText = Message("messages__psaSchemeDash__invite_link")
      ))
    } else {
      Nil
    }

  private def latestPsaSubHeading(ua: UserAnswers)(implicit messages: Messages): Seq[CardSubHeading] =
    latestPsa(ua).fold[Seq[CardSubHeading]](Nil) { psa =>
      Seq(CardSubHeading(
        subHeading = psa.relationshipDate.fold(messages("messages__psaSchemeDash__added"))(date =>
          messages("messages__psaSchemeDash__addedOn", LocalDate.parse(date).format(formatter))),
        subHeadingClasses = "card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = psa.getPsaName.getOrElse(throw PsaNameCannotBeRetrievedException),
          subHeadingParamClasses = "font-small bold"))))
    }

  private def latestPsa(ua: UserAnswers): Option[PsaDetails] =
    ua.get(ListOfPSADetailsId) flatMap (_.sortBy(_.relationshipDate).reverse.headOption)

  //PSP card
  def pspCard(ua: UserAnswers, schemeStatus: Option[String])
             (implicit messages: Messages): Seq[CardViewModel] =
    schemeStatus match {
      case Some(Open.value) =>
        Seq(CardViewModel(
          id = "psp_list",
          heading = Message("messages__psaSchemeDash__psp_heading"),
          subHeadings = latestPspSubHeading(ua),
          links = Seq(
            Link("authorise", WhatYouWillNeedController.onPageLoad().url, Message("messages__pspAuthorise__link"))
          ) ++ viewPspLink(ua)
        ))
      case _ =>
        Nil
    }


  private def viewPspLink(ua: UserAnswers): Seq[Link] =
    latestPsp(ua).fold[Seq[Link]](Nil) { _ =>
      Seq(Link(
        id = "view-practitioners",
        url = ViewPractitionersController.onPageLoad().url,
        linkText = Message("messages__pspViewOrDeauthorise__link")
      ))
    }

  private def latestPsp(ua: UserAnswers): Option[AuthorisedPractitioner] =
    ua.get(SeqAuthorisedPractitionerId) flatMap { seqPsp =>
      implicit val localDateOrdering: Ordering[LocalDate] = _ compareTo _
      seqPsp.sortBy(_.relationshipStartDate).reverse.headOption
    }


  private def latestPspSubHeading(ua: UserAnswers)(implicit messages: Messages): Seq[CardSubHeading] =
    latestPsp(ua).fold[Seq[CardSubHeading]](Nil) { psp =>
      Seq(CardSubHeading(
        subHeading = Message("messages__psaSchemeDash__addedOn", psp.relationshipStartDate.format(formatter)),
        subHeadingClasses = "card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = psp.name,
          subHeadingParamClasses = "font-small bold"))))
    }


  private def isSchemeOpen(ua: UserAnswers): Boolean = ua.get(SchemeStatusId).getOrElse("").equalsIgnoreCase("open")

  private def optToSeq[A](value: Option[A]): Seq[A] = value.fold[Seq[A]](Nil)(x => Seq(x))
}

case object PsaNameCannotBeRetrievedException extends Exception

object PsaSchemeDashboardService {
  val minStartDateAsString = "2000-04-06"
  val maxEndDateAsString = s"${LocalDate.now().getYear + 1}-04-05"
}

