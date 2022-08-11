/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.invitations.psp.routes._
import controllers.invitations.routes._
import controllers.psa.routes._
import controllers.psp.routes._
import identifiers.psa.ListOfPSADetailsId
import identifiers.{SchemeStatusId, SeqAuthorisedPractitionerId}
import models.SchemeStatus.Open
import models._
import models.psa.PsaDetails
import play.api.Logger
import play.api.i18n.Messages
import utils.DateHelper._
import utils.UserAnswers
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}

import java.time.LocalDate

class PsaSchemeDashboardService @Inject()(
                                           appConfig: FrontendAppConfig
                                         ) {

  private val logger = Logger(classOf[PsaSchemeDashboardService])

  def cards(srn: String, lock: Option[Lock], list: ListOfSchemes, ua: UserAnswers)
           (implicit messages: Messages): Seq[CardViewModel] = {
    val currentScheme = getSchemeDetailsFromListOfSchemes(srn, list)
    Seq(schemeCard(srn, currentScheme, lock, ua)) ++ Seq(psaCard(srn, ua)) ++ pspCard(ua, currentScheme.map(_.schemeStatus))
  }

  //Scheme details card
  def schemeCard(srn: String, currentScheme: Option[SchemeDetails], lock: Option[Lock], ua: UserAnswers)
                (implicit messages: Messages): CardViewModel = {
    CardViewModel(
      id = "scheme_details",
      heading = Message("messages__psaSchemeDash__scheme_details_head"),
      subHeadings = optToSeq(pstrSubHead(currentScheme)) ++ optToSeq(dateSubHead(currentScheme, ua)),
      links = Seq(schemeDetailsLink(srn, ua, lock))
    )
  }

  private def schemeDetailsLink(srn: String, ua: UserAnswers, lock: Option[Lock])
                               (implicit messages: Messages): Link = {
    val viewOrChangeLinkText = messages("messages__psaSchemeDash__view_change_details_link")
    val viewLinkText = messages("messages__psaSchemeDash__view_details_link")
    val linkText = if (!isSchemeOpen(ua)) {
      viewLinkText
    } else {

      logger.warn(s"Pension-scheme : $srn -- Lock-Status : ${lock.getOrElse("No-Lock-Found").toString}")

      ua.get(ListOfPSADetailsId).map {
        listOfPSADetails =>
          listOfPSADetails.map { psaDetails =>
            logger.warn(s"Pension-scheme : $srn -- PsaDetails-ID : ${psaDetails.id}")
          }
      }

      lock match {
        case Some(VarianceLock) | None => viewOrChangeLinkText
        case Some(_) => viewLinkText
      }
    }
    Link("view-details", appConfig.viewSchemeDetailsUrl.format(srn), linkText)
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
