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
import controllers.SchemeDetailsControllerSpec.{administrators, fakeRequest, frontendAppConfig, messages, openDate}
import controllers.actions.{DataRetrievalAction, _}
import models.{PsaDetails, PsaSchemeDetails}
import org.mockito.Matchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{contentAsString, _}
import views.html.schemeDetails
import testhelpers.CommonBuilders._

import scala.concurrent.Future

class SchemeDetailsControllerSpec extends ControllerSpecBase{
  import SchemeDetailsControllerSpec._

  appRunning()

  "SchemeDetailsController" must {
    "return OK and the correct view for a GET" in {
      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(schemeDetailsWithPsaOnlyResponse))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return OK and the correct view for a GET where administrators a mix of individual and org" in {
      val administrators = Some(Seq("partnetship name 2", "Smith A Tony"))
      def viewAsString(openDate: Option[String] = openDate, administrators: Option[Seq[String]] = administrators, isSchemeOpen: Boolean = true): String =
        schemeDetails(
          frontendAppConfig,
          mockSchemeDetails.name,
          openDate,
          administrators,
          srn,
          isSchemeOpen
        )(fakeRequest, messages).toString()

      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(schemeDetailsWithPsaOnlyResponseMixOfIndividualAndOrg))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return OK and the correct view for a GET when opened date is not returned by API" in {
      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(schemeDetailsWithPsaOnlyResponse))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesPartialResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(None)
    }

    "return OK and the correct view for a GET when scheme status is not open" in {
      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(schemeDetailsPendingResponse))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(openDate = None, isSchemeOpen = false)
    }

    "return NOT_FOUND when PSA data is not returned by API (as we don't know who administers the scheme)" in {
      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(schemeDetailsWithoutPsaResponse))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe NOT_FOUND
    }

    "return NOT_FOUND when PSA data is returned by API which does not include the currently logged-in PSA" in {
      val psaDetails1 = PsaDetails("A0000001",Some("partnership name no 1"),None)
      val psaDetails2 = PsaDetails("A0000002",Some("partnership name no 2"),None)
      val psaSchemeDetailsResponseTwoPSAs = PsaSchemeDetails(mockSchemeDetails, None, None, Some(Seq(psaDetails1, psaDetails2)))

      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(psaSchemeDetailsResponseTwoPSAs))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe NOT_FOUND
    }

  }
}

private object SchemeDetailsControllerSpec extends ControllerSpecBase with MockitoSugar {

  override lazy val app = new GuiceApplicationBuilder().configure(
    "features.work-package-one-enabled" -> true
  ).build()

  val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  val fakeListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData): SchemeDetailsController =
    new SchemeDetailsController(frontendAppConfig,
      messagesApi,
      fakeSchemeDetailsConnector,
      fakeListOfSchemesConnector,
      FakeAuthAction(),
      dataRetrievalAction,
      FakeUserAnswersCacheConnector)

  val schemeName = "Test Scheme Name"
  val administrators = Some(Seq("Taylor Middle Rayon", "Smith A Tony"))
  val openDate = Some("10 October 2012")
  val srn = "S1000000456"

  def viewAsString(openDate: Option[String] = openDate, administrators: Option[Seq[String]] = administrators, isSchemeOpen: Boolean = true): String =
    schemeDetails(
    frontendAppConfig,
    mockSchemeDetails.name,
    openDate,
    administrators,
    srn,
    isSchemeOpen
  )(fakeRequest, messages).toString()
}
