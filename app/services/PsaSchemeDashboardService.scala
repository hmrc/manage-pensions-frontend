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
import models.FeatureToggle.Enabled
import models.FeatureToggleName.PSPAuthorisation
import models.{Link, ListOfSchemes, Lock, VarianceLock}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper
import viewmodels.{CardSubHeading, CardSubHeadingParam, CardViewModel, Message}

import scala.concurrent.{ExecutionContext, Future}

class PsaSchemeDashboardService @Inject()(
                                           appConfig: FrontendAppConfig,
                                           featureToggleService: FeatureToggleService
                                         )(implicit ec: ExecutionContext) {

  def cards(srn: String, lock: Option[Lock])
           (implicit hc: HeaderCarrier,
            ec: ExecutionContext,
            messages: Messages): Future[Seq[CardViewModel]] =
    for {
      pspCard <- getPspCard(Some(""))
    } yield Seq(psaCard(true, srn, Some(""))) ++ pspCard

  //Scheme details card
  private def schemeCard(srn: String, isSchemeOpen: Boolean, list: ListOfSchemes, lock: Option[Lock])(implicit messages: Messages) =
    CardViewModel(
      id = "scheme_details",
      heading = Message("messages__psaSchemeDash__scheme_details_head"),
      subHeadings = optToSeq(pstrSubHead(srn, list)) ++ optToSeq(dateSubHead(srn, list, isSchemeOpen)),
      links = Seq(schemeDetailsLink(srn, isSchemeOpen, lock))
    )

  private def schemeDetailsLink(srn: String, isSchemeOpen: Boolean, lock: Option[Lock])
                               (implicit messages: Messages): Link = {
    val viewOrChangeLinkText = messages("messages__psaSchemeDash__view_change_details_link")
    val viewLinkText = messages("messages__psaSchemeDash__view_details_link")
    val linkText = if (!isSchemeOpen) {
      viewLinkText
    } else {
      lock match {
        case Some(VarianceLock) | None => viewOrChangeLinkText
        case Some(_) => viewLinkText
      }
    }
    Link("view-details", appConfig.viewSchemeDetailsUrl.format(srn), linkText)
  }

  def dateSubHead(srn: String, list: ListOfSchemes, isSchemeOpen: Boolean)
                 (implicit messages: Messages): Option[CardSubHeading] =
    if (isSchemeOpen) {
      list.schemeDetails.flatMap {
        listOfSchemes =>
          val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
          def date: Option[String] = currentScheme.head.openDate.map(date => LocalDate.parse(date).format(DateHelper.formatter))
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

  def pstrSubHead(srn: String, list: ListOfSchemes)(implicit messages: Messages): Option[CardSubHeading] =
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
  private def psaCard(isSchemeOpen: Boolean, srn: String, latestPsa: Option[String])
                     (implicit messages: Messages): CardViewModel =
    CardViewModel(
      id = "psa_list",
      heading = Message("messages__psaSchemeDash__psa_list_head"),
      subHeadings = latestPsaSubHeading(latestPsa),
      links = invitePsaLink(isSchemeOpen, srn) ++ Seq(
        Link("view-psa-list",
          controllers.routes.ViewAdministratorsController.onPageLoad(srn).url,
          Message("messages__psaSchemeDash__view_psa"))
      )
    )

  def invitePsaLink(isSchemeOpen: Boolean, srn: String): Seq[Link] =
    if(isSchemeOpen) {
      Seq(Link(
        id = "invite",
        url = controllers.invitations.routes.InviteController.onPageLoad(srn).url,
        linkText = Message("messages__psaSchemeDash__invite_link")
      ))
    } else {
      Nil
    }

  def latestPsaSubHeading(latestPsa: Option[String])(implicit messages: Messages): Seq[CardSubHeading] =
    latestPsa.fold[Seq[CardSubHeading]](Nil) { psa =>
      Seq(CardSubHeading(
        subHeading = Message("messages__psaSchemeDash__addedOn"),
        subHeadingClasses = "heading-small card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = psa,
          subHeadingParamClasses = "font-small"))))
    }

  //PSP card
  private def getPspCard(latestPsp: Option[String])
                        (implicit hc: HeaderCarrier, ec: ExecutionContext, messages: Messages):Future[Seq[CardViewModel]] =
    featureToggleService.get(PSPAuthorisation).map {
      case Enabled(PSPAuthorisation) =>

        Seq(CardViewModel(
          id = "psp_list",
          heading = Message("messages__psaSchemeDash__psp_heading"),
          subHeadings = latestPspSubHeading(latestPsp),
          links = Seq(
            Link("authorise", controllers.invitations.psp.routes.WhatYouWillNeedController.onPageLoad().url, Message("messages__pspAuthorise__link"))
          ) ++ viewPspLink(latestPsp)
        ))
      case _ =>
        Nil
    }

  def viewPspLink(latestPsp: Option[String]): Seq[Link] = latestPsp.fold[Seq[Link]](Nil) { _ =>
    Seq(Link(
      id = "view-practitioners",
      url = controllers.psp.routes.ViewPractitionersController.onPageLoad().url,
      linkText = Message("messages__pspViewOrDeauthorise__link")
    ))
  }


  def latestPspSubHeading(latestPsp: Option[String])(implicit messages: Messages): Seq[CardSubHeading] =
    latestPsp.fold[Seq[CardSubHeading]](Nil) { psp =>
      Seq(CardSubHeading(
        subHeading = Message("messages__psaSchemeDash__addedOn"),
        subHeadingClasses = "heading-small card-sub-heading",
        subHeadingParams = Seq(CardSubHeadingParam(
          subHeadingParam = psp,
          subHeadingParamClasses = "font-small"))))
    }

  def optToSeq[A](value: Option[A]): Seq[A] = value.fold[Seq[A]](Nil)(x => Seq(x))
}
