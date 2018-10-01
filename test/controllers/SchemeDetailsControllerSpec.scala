/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import connectors._
import controllers.actions.{DataRetrievalAction, _}
import models.{IndividualDetails, MinimalPSA, MinimalSchemeDetail, NormalMode}
import org.mockito.Matchers._
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{contentAsString, _}
import testhelpers.CommonBuilders
import views.html.schemeDetails
import identifiers.SchemeDetailId

import scala.concurrent.Future

class SchemeDetailsControllerSpec extends ControllerSpecBase {
  import SchemeDetailsControllerSpec._

  appRunning()

  "SchemeDetailsController" when {

    "on a GET" must {

      "return OK and the correct view" in {
        when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.schemeDetailsWithPsaOnlyResponse))
        when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.listOfSchemesResponse))
        val result = controller().onPageLoad(srn)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "save the minimal scheme details data and return OK with the correct view" in {
        when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.schemeDetailsWithPsaOnlyResponse))
        when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.listOfSchemesResponse))
        val result = controller().onPageLoad(srn)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
        FakeUserAnswersCacheConnector.verify(SchemeDetailId, minimalSchemeDetail)
      }

      "return OK and the correct view when opened date is not returned by API" in {
        when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.schemeDetailsWithPsaOnlyResponse))
        when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.listOfSchemesPartialResponse))
        val result = controller().onPageLoad(srn)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString(None)
      }

      "return OK and the correct view when scheme status is not open" in {
        when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.schemeDetailsPendingResponse))
        when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.listOfSchemesResponse))
        val result = controller().onPageLoad(srn)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString(openDate = None, isSchemeOpen = false)
      }

      "return OK and the correct view when PSA data is not returned by API" in {
        when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.schemeDetailsWithoutPsaResponse))
        when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
          .thenReturn(Future.successful(CommonBuilders.listOfSchemesResponse))
        val result = controller().onPageLoad(srn)(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString(administrators = None)
      }
    }

    "on onClickCheckIfPsaCanInvite " must {

      "redirect to cannot send an invite page if user is suspended " in {
        when(fakeMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPsa(isSuspended = true)))
        val result = controller().onClickCheckIfPsaCanInvite()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.invitations.routes.YouCannotSendAnInviteController.onPageLoad().url
      }

      "redirect to psa name page if user is not suspended " in {
        when(fakeMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minimalPsa(isSuspended = false)))
        val result = controller().onClickCheckIfPsaCanInvite()(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.invitations.routes.PsaNameController.onPageLoad(NormalMode).url
      }
    }
  }
}

private object SchemeDetailsControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {
  private def minimalPsa(isSuspended: Boolean) = MinimalPSA("test@test.com", isSuspended, None, Some(IndividualDetails("First",Some("Middle"),"Last")))

  override lazy val app = new GuiceApplicationBuilder().configure(
    "features.work-package-one-enabled" -> true
  ).build()

  val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  val fakeListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  val fakeMinimalPsaConnector: MinimalPsaConnector = mock[MinimalPsaConnector]

  override def beforeEach(): Unit = {
    reset(fakeSchemeDetailsConnector)
    reset(fakeMinimalPsaConnector)
    super.beforeEach()
  }

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData): SchemeDetailsController =
    new SchemeDetailsController(frontendAppConfig,
      messagesApi,
      fakeSchemeDetailsConnector,
      fakeListOfSchemesConnector,
      fakeMinimalPsaConnector,
      FakeUserAnswersCacheConnector,
      FakeAuthAction(),
      dataRetrievalAction)

  val schemeName = "Test Scheme Name"
  val administrators = Some(Seq("Taylor Middle Rayon", "Smith A Tony"))
  val openDate = Some("10 October 2012")
  val srn = "S1000000456"
  val minimalSchemeDetail = MinimalSchemeDetail(CommonBuilders.schemeDetails.srn, CommonBuilders.schemeDetails.pstr, CommonBuilders.schemeDetails.schemeName)

  def viewAsString(openDate: Option[String] = openDate, administrators: Option[Seq[String]] = administrators, isSchemeOpen: Boolean = true): String =
    schemeDetails(
    frontendAppConfig,
    CommonBuilders.schemeDetails.schemeName,
    openDate,
    administrators,
    srn,
    isSchemeOpen
  )(fakeRequest, messages).toString
}
