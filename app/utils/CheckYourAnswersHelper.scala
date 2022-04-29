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

package utils

import controllers.invitations.psa.routes._
import controllers.invitations.psp.routes._
import identifiers.invitations._
import identifiers.invitations.psa.{AdviserAddressId, AdviserEmailId, AdviserNameId, InviteePSAId}
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import models.{Address, CheckMode}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.countryOptions.CountryOptions


class CheckYourAnswersHelper(userAnswers: UserAnswers, countryOptions: CountryOptions) extends Enumerable.Implicits {

  def psaName(implicit messages: Messages): Option[SummaryListRow] = {
    userAnswers.get(InviteeNameId) map { answer =>
      SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psa__name__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(answer)),
        actions = Some(Actions("", items = Seq(ActionItem(href = PsaNameController.onPageLoad(CheckMode).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psa__name__label"))))))
      )
    }
  }

  def psaId(implicit messages: Messages): Option[SummaryListRow] = {
    userAnswers.get(InviteePSAId) map { answer =>
      SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psa__id__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(answer)),
        actions = Some(Actions("", items = Seq(ActionItem(href = PsaIdController.onPageLoad(CheckMode).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psa__id__label"))))))
      )
    }
  }

  def adviserName(implicit messages: Messages): Option[SummaryListRow] = {
    userAnswers.get(AdviserNameId) map { answer =>
      SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__adviser__name__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(answer)),
        actions = Some(Actions("", items = Seq(ActionItem(href = AdviserDetailsController.onPageLoad(CheckMode).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__adviser__name__label"))))))
      )
    }
  }

  def adviserEmail(label: String)(implicit messages: Messages): Option[SummaryListRow] = {
    userAnswers.get(AdviserEmailId) map { answer =>
      SummaryListRow(
        key = Key(Text(label), classes = "govuk-!-width-one-half"),
        value = Value(Text(answer)),
        actions = Some(Actions("", items = Seq(ActionItem(href = AdviserEmailAddressController.onPageLoad(CheckMode).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(label)))))
      )
    }
  }

  def adviserAddress(label: String)(implicit messages: Messages): Option[SummaryListRow] = {
    userAnswers.get(AdviserAddressId) map { answer =>
      SummaryListRow(
        key = Key(Text(label), classes = "govuk-!-width-one-half"),
        value = Value(Text(addressAnswer(answer).mkString(", "))),
        actions = Some(Actions("", items = Seq(ActionItem(href = AdviserManualAddressController.onPageLoad(CheckMode, true).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(label)))))
      )
    }

  }

  def addressAnswer(address: Address): Seq[String] = {
    val country = countryOptions.options.find(_.value == address.country).map(_.label).getOrElse(address.country)
    Seq(Some(address.addressLine1), Some(address.addressLine2), address.addressLine3,
      address.addressLine4, address.postcode, Some(country)).flatten
  }

  def pspName(implicit messages: Messages): Option[SummaryListRow] = {
    userAnswers.get(PspNameId) map { answer =>
      SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psp__name__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(answer)),
        actions = Some(Actions("", items = Seq(ActionItem(href = PspNameController.onPageLoad(CheckMode).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp__name__label"))))))
      )
    }
  }

  def pspId(implicit messages: Messages): Option[SummaryListRow] = {
    userAnswers.get(PspId) map { answer =>
      SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psp__id__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(answer)),
        actions = Some(Actions("", items = Seq(ActionItem(href = PspIdController.onPageLoad(CheckMode).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp__id__label"))))))
      )
    }
  }

  def pspClientReference(implicit messages: Messages): Option[SummaryListRow] = {
      Some(SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psp_client_reference__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(messages(getClientReference))),
        actions = Some(Actions("", items = Seq(ActionItem(href = PspHasClientReferenceController.onPageLoad(CheckMode).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp_client_reference__label"))))))
      ))
  }

  private def getClientReference: String = userAnswers.get(PspClientReferenceId).getOrElse("messages__none")
}
