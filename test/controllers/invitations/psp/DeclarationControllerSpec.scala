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
import connectors.PspConnector
import connectors.scheme.ListOfSchemesConnector
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.invitations.psp.DeclarationFormProvider
import identifiers.SchemeSrnId
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import models.ListOfSchemes
import models.invitations.psp.ClientReference
import models.invitations.psp.ClientReference.HaveClientReference
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, RecoverMethods}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers._
import services.SchemeDetailsService
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers
import views.html.invitations.psp.declaration

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach with JsonFileReader
  with RecoverMethods {

  def onwardRoute: Call = controllers.invitations.psp.routes.ConfirmationController.onPageLoad()

  val formProvider = new DeclarationFormProvider()
  val form: Form[Boolean] = formProvider()

  val config: Configuration = injector.instanceOf[Configuration]
  private val view = injector.instanceOf[declaration]
  private def sessionExpired: String = controllers.routes.SessionExpiredController.onPageLoad().url

  private val mockPspConnector = mock[PspConnector]
  private val mockListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val mockSchemeDetailsService = mock[SchemeDetailsService]

  val srn: String = "srn"
  val pstr: String = "pstr"
  val pspName: String = "psp-name"
  val pspId: String = "psp-id"
  val pspCR: ClientReference = HaveClientReference("psp-client-reference")

  val userAnswers: UserAnswers = UserAnswers().set(SchemeSrnId)(srn).asOpt.value
    .set(PspNameId)(pspName).asOpt.value
    .set(PspId)(pspId).asOpt.value
    .set(PspClientReferenceId)(pspCR).asOpt.value

  val data: DataRetrievalAction = new FakeDataRetrievalAction(Some(userAnswers.json))
  val listOfSchemesResponse: Future[Right[Nothing, ListOfSchemes]] = Future.successful(Right(ListOfSchemes("", "", None)))

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) = new DeclarationController(
    messagesApi,
    formProvider,
    FakeAuthAction(),
    dataRetrievalAction,
    new DataRequiredActionImpl,
    mockPspConnector,
    mockListOfSchemesConnector,
    mockSchemeDetailsService,
    stubMessagesControllerComponents(),
    view
  )

  private def viewAsString(form: Form[_] = form) = view(form)(fakeRequest, messages).toString

  "Declaration Controller" when {

    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad()(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }
    }

    "on a POST" must {

      "invite psp and redirect to confirmation page when valid data is submitted" in {

        when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(listOfSchemesResponse)
        when(mockSchemeDetailsService.pstr(any(), any())).thenReturn(Some(pstr))
        when(mockPspConnector.authorisePsp(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(data).onSubmit()(fakeRequest.withFormUrlEncodedBody("agree" -> "agreed"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url
        }

      "return Bad Request if invalid data is submitted" in {
        val formWithErrors = form.withError("agree", messages("messages__error__psp_declaration__required"))
        val result = controller().onSubmit()(fakeRequest)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(formWithErrors)
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onSubmit()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }
    }
  }
}
