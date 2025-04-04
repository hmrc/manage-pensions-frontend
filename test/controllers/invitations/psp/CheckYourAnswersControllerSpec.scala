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

package controllers.invitations.psp

import base.JsonFileReader
import connectors.admin.{MinimalConnector, PspUserNameNotMatchedException}
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.invitations.psp.routes._
import controllers.psa.routes.PsaSchemeDashboardController
import identifiers.invitations.psp.{PspClientReferenceId, PspHasClientReferenceId, PspId, PspNameId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{CheckMode, SchemeReferenceNumber}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.countryOptions.CountryOptions
import utils.{CheckYourAnswersFactory, UserAnswers}
import views.html.invitations.psp.checkYourAnswersPsp

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with MockitoSugar {

  import CheckYourAnswersControllerSpec._

  private val mockMinConnector = mock[MinimalConnector]

  def controller(dataRetrievalAction: DataRetrievalAction = data) = new CheckYourAnswersController(
    messagesApi, FakeAuthAction, dataRetrievalAction, new DataRequiredActionImpl,
    checkYourAnswersFactory, mockMinConnector, controllerComponents, view, fakePsaSchemeAuthAction
  )

  "Check Your Answers Controller Spec" must {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad(srn)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to the session expired page if there is no psp name" in {
        val result = controller(getEmptyData).onPageLoad(srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onPageLoad(srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }

    "on a POST" must {
      "redirect to Declaration if pspName matches the one email invitation API" in {
        when(mockMinConnector.getEmailInvitation(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(pspEmailAddress)))
        val result = controller(data).onSubmit(srn)(fakeRequest)
        redirectLocation(result).value mustBe DeclarationController.onPageLoad(srn).url
      }

      "redirect to interrupt if pspName does not match the one returned from minDetails API" in {
        when(mockMinConnector.getEmailInvitation(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.failed(new PspUserNameNotMatchedException))
        val result = controller(data).onSubmit(srn)(fakeRequest)
        redirectLocation(result).get mustBe PspDoesNotMatchController.onPageLoad(srn).url
      }
    }
  }
}

object CheckYourAnswersControllerSpec extends ControllerWithNormalPageBehaviours with MockitoSugar with JsonFileReader {
  private val pspName: String = "test-psp"
  private val pspEmailAddress: String = "psp@test.com"
  private val testSchemeName = "test-scheme-name"

  private val data = UserAnswers()
    .set(PspNameId)(pspName).asOpt.value
    .set(PspId)("A1231231").asOpt.value
    .set(PspHasClientReferenceId)(true).asOpt.value
    .set(PspClientReferenceId)("1234567").asOpt.value
    .set(SchemeNameId)(testSchemeName).asOpt.value
    .set(SchemeSrnId)(srn).asOpt.value
    .dataRetrievalAction

  private val expectedValues = Seq(
    SummaryListRow(
      key = Key(Text(messages("messages__check__your__answer__psp__name__label")), classes = "govuk-!-width-one-half"),
      value = Value(Text(pspName)),
      actions = Some(Actions("", items = Seq(ActionItem(href = PspNameController.onPageLoad(CheckMode, srn).url,
        content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp__name__label"))))))
    ),
    SummaryListRow(
      key = Key(Text(messages("messages__check__your__answer__psp__id__label")), classes = "govuk-!-width-one-half"),
      value = Value(Text("A1231231")),
      actions = Some(Actions("", items = Seq(ActionItem(href = PspIdController.onPageLoad(CheckMode, srn).url,
        content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp__id__label"))))))
    ),
    SummaryListRow(
      key = Key(Text(messages("messages__check__your__answer__psp_client_reference__label")), classes = "govuk-!-width-one-half"),
      value = Value(Text("1234567")),
      actions = Some(Actions("", items = Seq(ActionItem(href = PspHasClientReferenceController.onPageLoad(CheckMode, srn).url,
        content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp_client_reference__label"))))))
    )
  )

  private val countryOptions = new CountryOptions(environment, frontendAppConfig)
  private val checkYourAnswersFactory = new CheckYourAnswersFactory(countryOptions)

  private val view = injector.instanceOf[checkYourAnswersPsp]

  def call: Call = controllers.invitations.psp.routes.CheckYourAnswersController.onSubmit(srn)

  def returnCall: Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))

  def viewAsString(): String = view(expectedValues, call, Some("messages__check__your__answer__psp__label"),
    Some(testSchemeName), testSchemeName, returnCall)(fakeRequest, messages).toString


}
