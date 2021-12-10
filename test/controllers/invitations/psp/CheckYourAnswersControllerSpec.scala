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

package controllers.invitations.psp

import base.JsonFileReader
import connectors.admin.MinimalConnector
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.invitations.psp.routes._
import identifiers.SchemeNameId
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import models.CheckMode
import models.ClientReference.HaveClientReference
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.countryOptions.CountryOptions
import utils.{CheckYourAnswersFactory, PspAuthoriseFuzzyMatcher, UserAnswers}
import views.html.check_your_answers_view

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with MockitoSugar {

    import CheckYourAnswersControllerSpec._

    private val mockMinConnector = mock[MinimalConnector]

    private val mockPspAuthoriseFuzzyMatcher = mock[PspAuthoriseFuzzyMatcher]

    def controller(dataRetrievalAction: DataRetrievalAction = data) = new CheckYourAnswersController(
        messagesApi, FakeAuthAction, dataRetrievalAction, new DataRequiredActionImpl,
        checkYourAnswersFactory, mockMinConnector, mockPspAuthoriseFuzzyMatcher, controllerComponents, view
    )

    "Check Your Answers Controller Spec" must {
        "on a GET" must {

            "return OK and the correct view" in {
                val result = controller().onPageLoad()(fakeRequest)
                status(result) mustBe OK
                contentAsString(result) mustBe viewAsString()
            }

            "redirect to the session expired page if there is no psp name" in {
                val result = controller(getEmptyData).onPageLoad()(fakeRequest)
                redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
            }

            "redirect to the session expired page if there is no existing data" in {
                val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)
                redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
            }
        }

        "on a POST" must {
            "redirect to Declaration if pspName matches the one returned from minDetails API" in {
                when(mockMinConnector.getNameFromPspID(any())(any(), any())).thenReturn(Future.successful(Some(pspName)))
                when(mockPspAuthoriseFuzzyMatcher.matches(any(), any())).thenReturn(true)
                val result = controller(data).onSubmit()(fakeRequest)
                redirectLocation(result).value mustBe DeclarationController.onPageLoad().url
            }

            "redirect to interrupt if pspName does not match the one returned from minDetails API" in {
                when(mockMinConnector.getNameFromPspID(any())(any(), any())).thenReturn(Future.successful(Some(pspName)))
                when(mockPspAuthoriseFuzzyMatcher.matches(any(), any())).thenReturn(false)
                val result = controller(data).onSubmit()(fakeRequest)
                redirectLocation(result).get mustBe PspDoesNotMatchController.onPageLoad().url
            }
        }
    }
}

object CheckYourAnswersControllerSpec extends ControllerWithNormalPageBehaviours with MockitoSugar with JsonFileReader {
    private val pspName: String = "test-psp"
    private val testSchemeName = "test-scheme-name"

    private val data = UserAnswers()
            .set(PspNameId)(pspName).asOpt.value
            .set(PspId)("A1231231").asOpt.value
            .set(PspClientReferenceId)(HaveClientReference("1234567")).asOpt.value
            .set(SchemeNameId)(testSchemeName).asOpt.value
            .dataRetrievalAction

    private val expectedValues = Seq(
        SummaryListRow(
            key = Key(Text(messages("messages__check__your__answer__psp__name__label")), classes = "govuk-!-width-one-half"),
            value = Value(Text(pspName)),
            actions = Some(Actions("", items = Seq(ActionItem(href = PspNameController.onPageLoad(CheckMode).url,
                content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp__name__label"))))))
        ),
        SummaryListRow(
            key = Key(Text(messages("messages__check__your__answer__psp__id__label")), classes = "govuk-!-width-one-half"),
            value = Value(Text("A1231231")),
            actions = Some(Actions("", items = Seq(ActionItem(href = PspIdController.onPageLoad(CheckMode).url,
                content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp__id__label"))))))
        ),
        SummaryListRow(
            key = Key(Text(messages("messages__check__your__answer__psp_client_reference__label")), classes = "govuk-!-width-one-half"),
            value = Value(Text("1234567")),
            actions = Some(Actions("", items = Seq(ActionItem(href = PspClientReferenceController.onPageLoad(CheckMode).url,
                content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp_client_reference__label"))))))
        )
    )

    private val countryOptions = new CountryOptions(environment, frontendAppConfig)
    private val checkYourAnswersFactory = new CheckYourAnswersFactory(countryOptions)

    private val view = injector.instanceOf[check_your_answers_view]

    def call: Call = controllers.invitations.psp.routes.CheckYourAnswersController.onSubmit()

    def viewAsString(): String = view(expectedValues, call, Some("messages__check__your__answer__psp__label"),
        Some(testSchemeName))(fakeRequest, messages).toString



}
