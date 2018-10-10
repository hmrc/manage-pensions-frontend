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

import controllers.actions.{DataRetrievalAction, FakeDataRetrievalAction}
import identifiers.invitations._
import identifiers.{LastPageId, MinimalSchemeDetailId, SchemeSrnId}
import models._
import org.scalatest.OptionValues

package object utils {

  implicit class UserAnswerOps(answers: UserAnswers) extends OptionValues {

    def lastPage(page: LastPage): UserAnswers = {
      answers.set(LastPageId)(page).asOpt.value
    }

    // Invitation
    def inviteeId(id: String): UserAnswers = {
      answers.set(InviteePSAId)(id).asOpt.value
    }

    def inviteeName(name: String): UserAnswers = {
      answers.set(InviteeNameId)(name).asOpt.value
    }

    def minimalSchemeDetails(detail: MinimalSchemeDetail): UserAnswers = {
      answers.set(MinimalSchemeDetailId)(detail).asOpt.value
    }

    def schemeName(name: String): UserAnswers = {
      answers.set(SchemeNameId)(name).asOpt.value
    }

    def havePensionAdviser(hasAdviser: Boolean): UserAnswers = {
      answers.set(HaveYouEmployedPensionAdviserId)(hasAdviser).asOpt.value
    }

    def pstr(pstr: String): UserAnswers = {
      answers.set(PSTRId)(pstr).asOpt.value
    }

    def isMasterTrust(isMaster: Boolean): UserAnswers = {
      answers.set(IsMasterTrustId)(isMaster).asOpt.value
    }

    def srn(srn: String): UserAnswers = {
      answers.set(SchemeSrnId)(srn).asOpt.value
    }

    def adviserName(name: String): UserAnswers = {
      answers.set(AdviserNameId)(name).asOpt.value
    }

    def adviserAddress(address: Address): UserAnswers = {
      answers.set(AdviserAddressId)(address).asOpt.value
    }

    def adviserEmail(email: String): UserAnswers = {
      answers.set(AdviserEmailId)(email).asOpt.value
    }

    def employedPensionAdviserId(isChecked: Boolean): UserAnswers = {
      answers.set(HaveYouEmployedPensionAdviserId)(isChecked).asOpt.value
    }

    // Converters
    def dataRetrievalAction: DataRetrievalAction = {
      new FakeDataRetrievalAction(Some(answers.json))
    }

  }

}
