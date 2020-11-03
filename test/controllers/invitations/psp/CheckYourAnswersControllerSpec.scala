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

package controllers.invitations.psp

import base.JsonFileReader
import connectors.admin.MinimalConnector
import controllers.ControllerSpecBase
import controllers.actions.DataRequiredActionImpl
import controllers.actions.DataRetrievalAction
import controllers.actions.FakeAuthAction
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.invitations.psp.routes._
import identifiers.SchemeNameId
import identifiers.invitations.psp.PspClientReferenceId
import identifiers.invitations.psp.PspId
import identifiers.invitations.psp.PspNameId
import models.CheckMode
import models.invitations.psp.ClientReference.HaveClientReference
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.countryOptions.CountryOptions
import utils.CheckYourAnswersFactory
import utils.UserAnswers
import viewmodels.AnswerRow
import viewmodels.AnswerSection
import views.html.check_your_answers

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with MockitoSugar {

    import CheckYourAnswersControllerSpec._

    private val mockMinConnector = mock[MinimalConnector]
    def controller(dataRetrievalAction: DataRetrievalAction = data) = new CheckYourAnswersController(
        messagesApi, FakeAuthAction, dataRetrievalAction, new DataRequiredActionImpl,
        checkYourAnswersFactory, mockMinConnector, stubMessagesControllerComponents(), view
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
                val result = controller(data).onSubmit()(fakeRequest)
                redirectLocation(result).value mustBe DeclarationController.onPageLoad().url
            }

            "redirect to interrupt if pspName does not match the one returned from minDetails API" in {
                when(mockMinConnector.getNameFromPspID(any())(any(), any())).thenReturn(Future.successful(Some(testSchemeName)))
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

    private val expectedValues = List(AnswerSection(None,List(AnswerRow("messages__check__your__answer__psp__name__label",
        List(pspName),answerIsMessageKey = true,Some(PspNameController.onPageLoad(CheckMode).url)),
        AnswerRow("messages__check__your__answer__psp__id__label",List("A1231231"),answerIsMessageKey = true,
            Some(PspIdController.onPageLoad(CheckMode).url)),
        AnswerRow("messages__check__your__answer__psp_client_reference__label",List("1234567"),answerIsMessageKey = true,
            Some(PspClientReferenceController.onPageLoad(CheckMode).url)))))



    private val countryOptions = new CountryOptions(environment, frontendAppConfig)
    private val checkYourAnswersFactory = new CheckYourAnswersFactory(countryOptions)

    private val view = injector.instanceOf[check_your_answers]

    def call: Call = controllers.invitations.psp.routes.CheckYourAnswersController.onSubmit()

    def viewAsString(): String = view(expectedValues, None, call, Some("messages__check__your__answer__psp__label"),
        Some(testSchemeName), Some("site.save_and_continue"))(fakeRequest, messages).toString



}
