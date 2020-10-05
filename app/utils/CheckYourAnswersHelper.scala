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

package utils

import identifiers.invitations._
import identifiers.invitations.psp.PspClientReferenceId
import identifiers.invitations.psp.PspId
import identifiers.invitations.psp.PspNameId
import models.invitations.psp.ClientReference
import models.Address
import models.CheckMode
import utils.countryOptions.CountryOptions
import viewmodels.AnswerRow



class CheckYourAnswersHelper(userAnswers: UserAnswers, countryOptions: CountryOptions) extends Enumerable.Implicits {

  def psaName: Option[AnswerRow] = {
    userAnswers.get(InviteeNameId) map { answer =>
      AnswerRow("messages__check__your__answer__psa__name__label", Seq(answer), true,
        Some(controllers.invitations.routes.PsaNameController.onPageLoad(CheckMode).url))
    }
  }

  def psaId: Option[AnswerRow] = {
    userAnswers.get(InviteePSAId) map { answer =>
      AnswerRow("messages__check__your__answer__psa__id__label", Seq(answer), true,
        Some(controllers.invitations.routes.PsaIdController.onPageLoad(CheckMode).url))
    }
  }

  def adviserName: Option[AnswerRow] = {
    userAnswers.get(AdviserNameId) map { answer =>
      AnswerRow("messages__check__your__answer__adviser__name__label", Seq(answer),
        true, Some(controllers.invitations.routes.AdviserDetailsController.onPageLoad(CheckMode).url))
    }
  }

  def adviserEmail(label: String): Option[AnswerRow] = {
    userAnswers.get(AdviserEmailId) map { answer =>
      AnswerRow(label, Seq(answer),
        false, Some(controllers.invitations.routes.AdviserEmailAddressController.onPageLoad(CheckMode).url))
    }
  }

  def adviserAddress(label: String): Option[AnswerRow] = {
    userAnswers.get(AdviserAddressId) map { answer =>
      AnswerRow(label, addressAnswer(answer),
        false, Some(controllers.invitations.routes.AdviserManualAddressController.onPageLoad(CheckMode, true).url))
    }
  }

  def addressAnswer(address: Address): Seq[String] = {
    val country = countryOptions.options.find(_.value == address.country).map(_.label).getOrElse(address.country)
    Seq(Some(address.addressLine1), Some(address.addressLine2), address.addressLine3,
      address.addressLine4, address.postcode, Some(country)).flatten
  }

  def pspName: Option[AnswerRow] = {
    userAnswers.get(PspNameId) map { answer =>
      AnswerRow("messages__check__your__answer__psp__name__label", Seq(answer), true,
        Some(controllers.invitations.psp.routes.PspNameController.onPageLoad(CheckMode).url))
    }
  }

  def pspId: Option[AnswerRow] = {
    userAnswers.get(PspId) map { answer =>
      AnswerRow("messages__check__your__answer__psp__id__label", Seq(answer), true,
        Some(controllers.invitations.psp.routes.PspIdController.onPageLoad(CheckMode).url))
    }
  }

  def pspClientReference: Option[AnswerRow] = {
    userAnswers.get(PspClientReferenceId) map { answer =>
      AnswerRow("messages__check__your__answer__psp_client_reference__label", Seq(getClientReference(answer)), true,
        Some(controllers.invitations.psp.routes.PspClientReferenceController.onPageLoad(CheckMode).url))
    }
  }

  private def getClientReference(answer: ClientReference): String = answer match {
    case ClientReference.HaveClientReference(reference) => reference
    case ClientReference.NoClientReference => "messages__none"
  }
}
