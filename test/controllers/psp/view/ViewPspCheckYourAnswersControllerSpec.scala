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

package controllers.psp.view

import base.JsonFileReader
import connectors.UpdateClientReferenceConnector
import connectors.scheme.SchemeDetailsConnector
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.psa.routes.PsaSchemeDashboardController
import controllers.psp.view.routes._
import identifiers.invitations.PSTRId
import identifiers.psp.PspOldClientReferenceId
import identifiers.psp.deauthorise.PspDetailsId
import identifiers.{SchemeNameId, SchemeSrnId}
import models.psp._
import models.{CheckMode, SchemeReferenceNumber}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers._
import testhelpers.CommonBuilders.{pspDetails, pspDetails2}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.UserAnswers
import views.html.invitations.psp.checkYourAnswersPsp

import scala.concurrent.Future

class ViewPspCheckYourAnswersControllerSpec extends ControllerSpecBase with MockitoSugar with PrivateMethodTester {

  import ViewPspCheckYourAnswersControllerSpec._


  private val mockUpdateClientReferenceConnector: UpdateClientReferenceConnector = mock[UpdateClientReferenceConnector]
  private val mockSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]


  def controller(dataRetrievalAction: DataRetrievalAction = data) = new ViewPspCheckYourAnswersController(
    messagesApi, FakeAuthAction, dataRetrievalAction, new DataRequiredActionImpl,
    mockUpdateClientReferenceConnector, mockSchemeDetailsConnector, controllerComponents, view, fakePsaSchemeAuthAction
  )

  "Check Your Answers Controller Spec" must {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller(data).onPageLoad(0, srn)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to the session expired page if there is no psp name" in {
        val result = controller(getEmptyData).onPageLoad(0, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
      "redirect to the session expired page if not authorisingPSA" in {
        val result = controller(data2).onPageLoad(0, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onPageLoad(0, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }

    "on a POST" must {
      "redirect to view practitioner and post to update client ref API" in {
        when(mockUpdateClientReferenceConnector.updateClientReference(any(), any())(any(), any())).thenReturn(Future.successful("Ok"))
        when(mockSchemeDetailsConnector.getSchemeDetails(any(), any(), any())(any(), any())).thenReturn(Future.successful(schemeDetailUserAns("Test")))
        when(mockSchemeDetailsConnector.getSchemeDetailsRefresh(any(), any(), any())(any(), any())).thenReturn(Future.successful((): Unit))
        val result = controller(data).onSubmit(0, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.psp.routes.ViewPractitionersController.onPageLoad(srn).url
      }
      "redirect to view practitioner and not updated client Ref" in {
        when(mockSchemeDetailsConnector.getSchemeDetails(any(), any(), any())(any(), any())).thenReturn(Future.successful(schemeDetailUserAns("A0000000")))
        val result = controller(data).onSubmit(0, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.psp.routes.ViewPractitionersController.onPageLoad(srn).url
      }
      "redirect to the session expired page if not authorisingPSA" in {
        val result = controller(data2).onSubmit(0, srn)(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }

    "determineUserAction" must {

      "return UserAction NoChange when client Reference has no change " in {

        val determineUserAction: PrivateMethod[ClientReferenceUserAction] = PrivateMethod[ClientReferenceUserAction](Symbol("determineUserAction"))
        val result: ClientReferenceUserAction = controller(data) invokePrivate determineUserAction(None, None)

        result mustBe NoChange
      }

      "return UserAction Added when client Reference has change form None to value" in {

        val determineUserAction: PrivateMethod[ClientReferenceUserAction] = PrivateMethod[ClientReferenceUserAction](Symbol("determineUserAction"))
        val result: ClientReferenceUserAction = controller(data) invokePrivate determineUserAction(Some("Test"), None)

        result mustBe Added
      }

      "return UserAction Amended when client Reference has change" in {

        val determineUserAction: PrivateMethod[ClientReferenceUserAction] = PrivateMethod[ClientReferenceUserAction](Symbol("determineUserAction"))
        val result: ClientReferenceUserAction = controller(data) invokePrivate determineUserAction(Some("Test"), Some("oldValue"))

        result mustBe Amended
      }

      "return UserAction Deleted when client Reference updated to None" in {

        val determineUserAction: PrivateMethod[ClientReferenceUserAction] = PrivateMethod[ClientReferenceUserAction](Symbol("determineUserAction"))
        val result: ClientReferenceUserAction = controller(data) invokePrivate determineUserAction(None, Some("oldValue"))

        result mustBe Deleted
      }

    }
  }
}

object ViewPspCheckYourAnswersControllerSpec extends ControllerWithNormalPageBehaviours with MockitoSugar with JsonFileReader {
  private val pspName: String = "test-psp-name"
  private val testSchemeName = "Test Scheme"
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

  private def schemeDetailUserAns(clientRef: String) = UserAnswers()
    .set(PspOldClientReferenceId(0))(clientRef).asOpt.value

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
      actions = Some(Actions("", items = Seq(ActionItem(href = ViewPspHasClientReferenceController.onPageLoad(CheckMode, 0, srn).url,
        content = Text(messages("site.change")), visuallyHiddenText = Some(messages("messages__check__your__answer__psp_client_reference__label"))))))
    )
  )

  private val view = injector.instanceOf[checkYourAnswersPsp]

  def call: Call = controllers.psp.view.routes.ViewPspCheckYourAnswersController.onSubmit(0, srn)

  def returnCall: Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))

  def viewAsString(): String = view(expectedValues, call, None,
    Some(testSchemeName), testSchemeName, returnCall)(fakeRequest, messages).toString

}


