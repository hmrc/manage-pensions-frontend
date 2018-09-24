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

import com.thoughtworks.selenium.webdriven.commands.IsChecked
import controllers.actions.{DataRetrievalAction, FakeDataRetrievalAction}
import identifiers.invitations.{HaveYouEmployedPensionAdviserId, AdviserNameId, PSAId, PsaNameId}
import identifiers.{LastPageId, SchemeDetailId}
import models._
import org.scalatest.OptionValues

package object utils {

  implicit class UserAnswerOps(answers: UserAnswers) extends OptionValues {

    def lastPage(page: LastPage): UserAnswers = {
      answers.set(LastPageId)(page).asOpt.value
    }

    // Invitation Id
    def inviteeId(id: String): UserAnswers = {
      answers.set(PSAId)(id).asOpt.value
    }

    // Invitation Name
    def inviteeName(name: String): UserAnswers = {
      answers.set(PsaNameId)(name).asOpt.value
    }

    def minimalSchemeDetails(detail: MinimalSchemeDetail): UserAnswers = {
      answers.set(SchemeDetailId)(detail).asOpt.value
    }

    def adviserId(id: String): UserAnswers = {
      answers.set(AdviserNameId)(id).asOpt.value
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
