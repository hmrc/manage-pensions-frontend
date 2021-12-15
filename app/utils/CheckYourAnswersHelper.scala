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

package utils

import controllers.invitations.psa.routes._
import controllers.invitations.psp.routes._
import identifiers.invitations._
import identifiers.invitations.psa.{AdviserAddressId, AdviserEmailId, AdviserNameId, InviteePSAId}
import identifiers.invitations.psp.PspClientReferenceId
import identifiers.invitations.psp.PspId
import identifiers.invitations.psp.PspNameId
import models.{Address, CheckMode, ClientReference}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow, Value, Actions, ActionItem}
import utils.countryOptions.CountryOptions
import viewmodels.AnswerRow



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

  def adviserName: Option[AnswerRow] = {
    userAnswers.get(AdviserNameId) map { answer =>
      AnswerRow("messages__check__your__answer__adviser__name__label", Seq(answer),
        true, Some(AdviserDetailsController.onPageLoad(CheckMode).url))
    }
  }

  def adviserEmail(label: String): Option[AnswerRow] = {
    userAnswers.get(AdviserEmailId) map { answer =>
      AnswerRow(label, Seq(answer),
        false, Some(AdviserEmailAddressController.onPageLoad(CheckMode).url))
    }
  }

  def adviserAddress(label: String): Option[AnswerRow] = {
    userAnswers.get(AdviserAddressId) map { answer =>
      AnswerRow(label, addressAnswer(answer),
        false, Some(AdviserManualAddressController.onPageLoad(CheckMode, true).url))
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
    userAnswers.get(PspClientReferenceId) map { answer =>
      SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psp_client_reference__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(messages(getClientReference(answer)))),
        actions = Some(Actions("", items = Seq(ActionItem(href = PspClientReferenceController.onPageLoad(CheckMode).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp_client_reference__label"))))))
      )
    }
  }

  private def getClientReference(answer: ClientReference): String = answer match {
    case ClientReference.HaveClientReference(reference) => reference
    case ClientReference.NoClientReference => "messages__none"
  }
}
