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

package utils

import base.SpecBase
import models.CheckMode
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._

class ViewPspCheckYourAnswersHelperSpec extends SpecBase with Matchers {

  val userAnswers: UserAnswers = UserAnswers()

  private val pspName = "pspName"
  private val pspId = "0000000"
  private val clientRef = "qaz123"

  def getHelper = new ViewPspCheckYourAnswersHelper()

  "calling pspName" must {

    "return answer row for pspName" in {

      getHelper.pspName(pspName) mustBe
        SummaryListRow(
          key = Key(Text(messages("messages__check__your__answer__psp__name__label")), classes = "govuk-!-width-one-half"),
          value = Value(Text(pspName)),
          actions = None
        )
    }
  }

  "calling pspId" must {
    "return answer row pspId" in {

      getHelper.pspId(pspId) mustBe
        SummaryListRow(
          key = Key(Text(messages("messages__check__your__answer__psp__id__label")), classes = "govuk-!-width-one-half"),
          value = Value(Text(pspId)),
          actions = None
        )
    }
  }

  "calling pspClientReference" must {
    "return answer row for ClientReference with Reference value " in {
      getHelper.pspClientReference(Some(clientRef), 0) mustBe SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psp_client_reference__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(clientRef)),
        actions = Some(Actions("", items = Seq(ActionItem(href = controllers.psp.view.routes.ViewPspHasClientReferenceController.onPageLoad(CheckMode, 0).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp_client_reference__label"))))))
      )
    }

    "return answer row for ClientReference with Reference None " in {
      getHelper.pspClientReference(None, 0) mustBe SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psp_client_reference__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(messages("messages__none"))),
        actions = Some(Actions("", items = Seq(ActionItem(href = controllers.psp.view.routes.ViewPspHasClientReferenceController.onPageLoad(CheckMode, 0).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp_client_reference__label"))))))
      )
    }
  }
}
