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
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.invitations.psp.routes._
import identifiers.SchemeNameId
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import models.invitations.psp.ClientReference.HaveClientReference
import models.{CheckMode, MinimalSchemeDetail}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.countryOptions.CountryOptions
import utils.{CheckYourAnswersFactory, FakeNavigator, UserAnswers}
import viewmodels.{AnswerRow, AnswerSection}
import views.html.check_your_answers

class CheckYourAnswersControllerSpec extends ControllerSpecBase with MockitoSugar {

    import CheckYourAnswersControllerSpec._


    def controller(dataRetrievalAction: DataRetrievalAction = minimalData) = new CheckYourAnswersController(
        frontendAppConfig, messagesApi, FakeAuthAction(), new FakeNavigator(onwardRoute),
        dataRetrievalAction, new DataRequiredActionImpl, checkYourAnswersFactory, stubMessagesControllerComponents(), view
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
    }
}



object CheckYourAnswersControllerSpec extends ControllerWithNormalPageBehaviours with MockitoSugar with JsonFileReader {
    private val testSrn: String = "test-srn"
    private val testPstr = "test-pstr"
    private val testSchemeName = "test-scheme-name"
    private val testSchemeDetail = MinimalSchemeDetail(testSrn, Some(testPstr), testSchemeName)
    private val srn = "S9000000000"

    private val minimalData = UserAnswers()
            .set(PspNameId)("Bob").asOpt.value
            .set(PspId)("A1231231").asOpt.value
            .set(PspClientReferenceId)(HaveClientReference("1234567")).asOpt.value
            .set(SchemeNameId)(testSchemeName).asOpt.value
            .dataRetrievalAction

    private val expectedValues = List(AnswerSection(None,List(AnswerRow("messages__check__your__answer__psp__name__label",
        List("Bob"),true,Some(PspNameController.onPageLoad(CheckMode).url)),
        AnswerRow("messages__check__your__answer__psp__id__label",List("A1231231"),true,
            Some(PspIdController.onPageLoad(CheckMode).url)),
        AnswerRow("messages__check__your__answer__psp_client_reference__label",List("1234567"),true,
            Some(PspClientReferenceController.onPageLoad(CheckMode).url)))))



    private val countryOptions = new CountryOptions(environment, frontendAppConfig)
    private val checkYourAnswersFactory = new CheckYourAnswersFactory(countryOptions)

    private val view = injector.instanceOf[check_your_answers]

    def call: Call = controllers.invitations.psp.routes.CheckYourAnswersController.onSubmit()

    def viewAsString() = view(expectedValues, None, call,
        Some("messages__check__your__answer__psp__label"), Some(testSchemeName), Some("site.save_and_continue"))(fakeRequest, messages).toString



}
