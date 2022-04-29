/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.psp.view

import base.JsonFileReader
import connectors.UpdateClientReferenceConnector
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.psa.routes.PsaSchemeDashboardController
import controllers.psp.view.routes._
import identifiers.invitations.PSTRId
import identifiers.psp.deauthorise.PspDetailsId
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{CheckMode, SchemeReferenceNumber}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers._
import testhelpers.CommonBuilders.{pspDetails, pspDetails2}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.UserAnswers
import views.html.invitations.psp.checkYourAnswersPsp

import scala.concurrent.Future

class ViewPspCheckYourAnswersControllerSpec extends ControllerSpecBase with MockitoSugar {

  import ViewPspCheckYourAnswersControllerSpec._


  private val mockUpdateClientReferenceConnector: UpdateClientReferenceConnector = mock[UpdateClientReferenceConnector]


  def controller(dataRetrievalAction: DataRetrievalAction = data) = new ViewPspCheckYourAnswersController(
    messagesApi, FakeAuthAction, dataRetrievalAction, new DataRequiredActionImpl,
    mockUpdateClientReferenceConnector, controllerComponents, view
  )

  "Check Your Answers Controller Spec" must {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller(data).onPageLoad(0)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to the session expired page if there is no psp name" in {
        val result = controller(getEmptyData).onPageLoad(0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
      "redirect to the session expired page if not authorisingPSA" in {
        val result = controller(data2).onPageLoad(0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onPageLoad(0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
    }

    "on a POST" must {
      "redirect to view practitioner and post to update client ref API" in {
        when(mockUpdateClientReferenceConnector.updateClientReference(any())(any(), any())).thenReturn(Future.successful("Ok"))
        val result = controller(data).onSubmit(0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.psp.routes.ViewPractitionersController.onPageLoad().url
      }
      "redirect to the session expired page if not authorisingPSA" in {
        val result = controller(data2).onSubmit(0)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
      }
    }
  }
}

object ViewPspCheckYourAnswersControllerSpec extends ControllerWithNormalPageBehaviours with MockitoSugar with JsonFileReader {
  private val pspName: String = "test-psp-name"
  private val testSchemeName = "Test Scheme"
  private val srn = "srn"
  private val pstr = "pstr"


  private val data = UserAnswers()
    .set(PspDetailsId(0))(pspDetails).asOpt.value
    .set(SchemeNameId)(testSchemeName).asOpt.value
    .set(PSTRId)(pstr).asOpt.value
    .set(SchemeSrnId)(srn).asOpt.value.
    dataRetrievalAction

  private val data2 = UserAnswers()
    .set(PspDetailsId(0))(pspDetails2).asOpt.value
    .set(SchemeNameId)(testSchemeName).asOpt.value
    .set(PSTRId)(pstr).asOpt.value
    .set(SchemeSrnId)(srn).asOpt.value.
    dataRetrievalAction

  private val expectedValues = Seq(
    SummaryListRow(
      key = Key(Text(messages("messages__check__your__answer__psp__name__label")), classes = "govuk-!-width-one-half"),
      value = Value(Text(pspName))
    ),
    SummaryListRow(
      key = Key(Text(messages("messages__check__your__answer__psp__id__label")), classes = "govuk-!-width-one-half"),
      value = Value(Text("00000000"))
    ),
    SummaryListRow(
      key = Key(Text(messages("messages__check__your__answer__psp_client_reference__label")), classes = "govuk-!-width-one-half"),
      value = Value(Text("A0000000")),
      actions = Some(Actions("", items = Seq(ActionItem(href = ViewPspHasClientReferenceController.onPageLoad(CheckMode, 0).url,
        content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp_client_reference__label"))))))
    )
  )

  private val view = injector.instanceOf[checkYourAnswersPsp]

  def call: Call = controllers.psp.view.routes.ViewPspCheckYourAnswersController.onSubmit(0)

  def returnCall: Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))

  def viewAsString(): String = view(expectedValues, call, None,
    Some(testSchemeName), testSchemeName, returnCall)(fakeRequest, messages).toString

}


