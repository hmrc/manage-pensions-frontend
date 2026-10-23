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

package controllers.invitations

import connectors.FakeUserAnswersCacheConnector
import connectors.admin.MinimalConnector
import controllers.ControllerSpecBase
import controllers.actions._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import utils.{UserAnswers, UserAnswerOps}
import views.html.invitations.invitationAccepted

import scala.concurrent.Future

class InvitationsAcceptedControllerSpec extends ControllerSpecBase with MockitoSugar {

  private val mockMinimalConnector = mock[MinimalConnector]

  private val psaName = "Nigel Robert Smith"

  val testSchemeName: String = "Test Scheme Name"
  val getRelevantData: DataRetrievalAction = UserAnswers().schemeName(testSchemeName).dataRetrievalAction
  private val view = injector.instanceOf[invitationAccepted]

  def controller(authAction: AuthAction = FakeAuthAction, dataRetrievalAction: DataRetrievalAction = getRelevantData):
  InvitationAcceptedController =
    new InvitationAcceptedController(
      messagesApi,
      authAction,
      dataRetrievalAction,
      new DataRequiredActionImpl,
      FakeUserAnswersCacheConnector,
      mockMinimalConnector,
      controllerComponents,
      view
    )

  def viewAsString(): String = view(testSchemeName, psaName)(using fakeRequest, messages).toString

  "InvitationAccepted Controller" must {

    "return OK with correct content on GET" in {
      when(mockMinimalConnector.getPsaNameFromPsaID()(using any(), any()))
        .thenReturn(Future.successful(Some(psaName)))

      val result = controller().onPageLoad(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
      val document = Jsoup.parse(contentAsString(result))
      document.select("main h2.govuk-heading-m").text() mustBe "What happens next"
      document.select("#return-to-schemes").size() mustBe 1
      document.select("#return-to-schemes").text() mustBe s"Return to $psaName"
      document.select("#return-to-schemes").attr("href") mustBe controllers.routes.SchemesOverviewController.onPageLoad().url
      document.select("p.govuk-body").text() must include(
        "It can take up to 1 hour before your pension scheme is available to view."
      )
      FakeUserAnswersCacheConnector.verifyAllDataRemoved()
    }

    "redirect to session expired if the administrator name is missing" in {
      when(mockMinimalConnector.getPsaNameFromPsaID()(using any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller().onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad.url)
    }

    "redirect to session expired if required data is missing" in {

      val result = controller(dataRetrievalAction = getEmptyData).onPageLoad(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad.url)
    }

    "redirect to unauthorised if user action is not authenticated" in {

      val result = controller(authAction = FakeUnAuthorisedAction).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }
  }
}
