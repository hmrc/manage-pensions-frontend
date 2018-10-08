/*
 * Copyright 2018 HM Revenue & Customs
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
import models.{Address, CheckMode}
import utils.countryOptions.CountryOptions
import viewmodels.AnswerRow



class CheckYourAnswersHelper(userAnswers: UserAnswers, countryOptions: CountryOptions) extends Enumerable.Implicits {

  def psaName: Option[AnswerRow] = {
    userAnswers.get(InviteeNameId) map { answer =>
      AnswerRow("messages__check__your__answer__psa__name__label", Seq(answer), true, Some(controllers.invitations.routes.PsaNameController.onPageLoad(CheckMode).url))
    }
  }

  def psaId: Option[AnswerRow] = {
    userAnswers.get(InviteePSAId) map { answer =>
      AnswerRow("messages__check__your__answer__psa__id__label", Seq(answer), true, Some(controllers.invitations.routes.PsaIdController.onPageLoad(CheckMode).url))
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
    val country = countryOptions.options.find(_.value == address.countryCode).map(_.label).getOrElse(address.countryCode)
    Seq(Some(s"${address.line1},"), Some(s"${address.line2},"), address.line3.map(line3 => s"$line3,"),
      address.line4.map(line4 => s"$line4,"), address.postalCode.map(postCode => s"$postCode,"), Some(country)).flatten
  }

}
