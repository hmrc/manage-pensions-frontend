/*
 * Copyright 2021 HM Revenue & Customs
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
import identifiers.{ListOfPSADetailsId, SchemeStatusId, SeqAuthorisedPractitionerId}
import models.FeatureToggle.Enabled
import models.FeatureToggleName.PSPAuthorisation
import models.{AuthorisedPractitioner, Link, ListOfSchemes, Lock, PsaDetails, VarianceLock}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper._
import utils.UserAnswers
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}

import scala.concurrent.{ExecutionContext, Future}

class PsaSchemeDashboardService @Inject()(
                                           appConfig: FrontendAppConfig,
                                           featureToggleService: FeatureToggleService
                                         )(implicit ec: ExecutionContext) {

  def cards(srn: String, lock: Option[Lock], list: ListOfSchemes, ua: UserAnswers)
           (implicit hc: HeaderCarrier,
            ec: ExecutionContext,
            messages: Messages): Future[Seq[CardViewModel]] =
    for {
      pspCard <- pspCard(ua)
    } yield Seq(schemeCard(srn, list, lock, ua)) ++ Seq(psaCard(srn, ua)) ++ pspCard

  //Scheme details card
  def schemeCard(srn: String, list: ListOfSchemes, lock: Option[Lock], ua: UserAnswers)
                        (implicit messages: Messages): CardViewModel =
    CardViewModel(
      id = "scheme_details",
      heading = Message("messages__psaSchemeDash__scheme_details_head"),
      subHeadings = optToSeq(pstrSubHead(srn, list)) ++ optToSeq(dateSubHead(srn, list, ua)),
      links = Seq(schemeDetailsLink(srn, ua, lock))
    )

  private def schemeDetailsLink(srn: String, ua: UserAnswers, lock: Option[Lock])
                               (implicit messages: Messages): Link = {
    val viewOrChangeLinkText = messages("messages__psaSchemeDash__view_change_details_link")
    val viewLinkText = messages("messages__psaSchemeDash__view_details_link")
    val linkText = if (!isSchemeOpen(ua)) {
      viewLinkText
    } else {
      lock match {
        case Some(VarianceLock) | None => viewOrChangeLinkText
        case Some(_) => viewLinkText
      }
    }
    Link("view-details", appConfig.viewSchemeDetailsUrl.format(srn), linkText)
  }

  private def dateSubHead(srn: String, list: ListOfSchemes, ua: UserAnswers)
                 (implicit messages: Messages): Option[CardSubHeading] =
    if (isSchemeOpen(ua)) {
      list.schemeDetails.flatMap {
        listOfSchemes =>
          val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
          def date: Option[String] = currentScheme.head.openDate.map(date => LocalDate.parse(date).format(formatter))
          if (currentScheme.nonEmpty && date.nonEmpty) {
            Some(CardSubHeading(
              subHeading = Message("messages__psaSchemeDash__regDate"),
              subHeadingClasses = "heading-small card-sub-heading",
              subHeadingParams = Seq(CardSubHeadingParam(
                subHeadingParam = date.getOrElse(""),
                subHeadingParamClasses = "font-small"))))
          } else {
            None
          }
      }
    }
    else {
      None
    }

  private def pstrSubHead(srn: String, list: ListOfSchemes)(implicit messages: Messages): Option[CardSubHeading] =
    list.schemeDetails.flatMap { listOfSchemes =>
      val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
      if (currentScheme.nonEmpty && currentScheme.head.pstr.nonEmpty) {
        Some(CardSubHeading(
          subHeading = Message("messages__psaSchemeDash__pstr"),
          subHeadingClasses = "heading-small card-sub-heading",
          subHeadingParams = Seq(CardSubHeadingParam(
            subHeadingParam = currentScheme.head.pstr.head,
            subHeadingParamClasses = "font-small"))))
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
        Link("view-psa-list",
          controllers.routes.ViewAdministratorsController.onPageLoad(srn).url,
          Message("messages__psaSchemeDash__view_psa"))
      )
    )

  private def invitePsaLink(isSchemeOpen: Boolean, srn: String): Seq[Link] =
    if(isSchemeOpen) {
      Seq(Link(
        id = "invite",
        url = controllers.invitations.routes.InviteController.onPageLoad(srn).url,
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
        subHeadingClasses = "heading-small card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = psa.getPsaName.getOrElse(throw PsaNameCannotBeRetrievedException),
          subHeadingParamClasses = "font-small"))))
    }

  private def latestPsa(ua: UserAnswers): Option[PsaDetails] =
    ua.get(ListOfPSADetailsId) flatMap { seqPsa =>
      implicit val localDateOrdering: Ordering[LocalDate] = _ compareTo _
      seqPsa.sortBy(_.relationshipDate).reverse.headOption
    }

  //PSP card
  def pspCard(ua: UserAnswers)
             (implicit hc: HeaderCarrier, ec: ExecutionContext, messages: Messages):Future[Seq[CardViewModel]] =
    featureToggleService.get(PSPAuthorisation).map {
      case Enabled(PSPAuthorisation) =>
        Seq(CardViewModel(
          id = "psp_list",
          heading = Message("messages__psaSchemeDash__psp_heading"),
          subHeadings = latestPspSubHeading(ua),
          links = Seq(
            Link("authorise", controllers.invitations.psp.routes.WhatYouWillNeedController.onPageLoad().url, Message("messages__pspAuthorise__link"))
          ) ++ viewPspLink(ua)
        ))
      case _ =>
        Nil
    }

  private def viewPspLink(ua: UserAnswers): Seq[Link] =
    latestPsp(ua).fold[Seq[Link]](Nil) { _ =>
      Seq(Link(
        id = "view-practitioners",
        url = controllers.psp.routes.ViewPractitionersController.onPageLoad().url,
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
        subHeadingClasses = "heading-small card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = psp.name,
          subHeadingParamClasses = "font-small"))))
    }

  private def isSchemeOpen(ua: UserAnswers): Boolean = ua.get(SchemeStatusId).getOrElse("").equalsIgnoreCase("open")

  private def optToSeq[A](value: Option[A]): Seq[A] = value.fold[Seq[A]](Nil)(x => Seq(x))
}

case object PsaNameCannotBeRetrievedException extends Exception
