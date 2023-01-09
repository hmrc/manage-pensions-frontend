/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.psp.view.routes._
import models.CheckMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._


class ViewPspCheckYourAnswersHelper extends Enumerable.Implicits {

  def pspName(name:String)(implicit messages: Messages): SummaryListRow = {
      SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psp__name__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(name)),
        actions = None
      )
  }

  def pspId(id:String)(implicit messages: Messages): SummaryListRow = {
      SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psp__id__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(id)),
        actions = None
      )
  }

  def pspClientReference(clientRef:Option[String],index:Int)(implicit messages: Messages): SummaryListRow = {
      SummaryListRow(
        key = Key(Text(messages("messages__check__your__answer__psp_client_reference__label")), classes = "govuk-!-width-one-half"),
        value = Value(Text(messages(getClientReference(clientRef)))),
        actions = Some(Actions("", items = Seq(ActionItem(href = ViewPspHasClientReferenceController.onPageLoad(CheckMode,index).url,
          content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp_client_reference__label"))))))
      )
  }

  private def getClientReference(clientReference: Option[String]): String = clientReference match {
    case Some(reference) => reference
    case _ => "messages__none"
  }
}
