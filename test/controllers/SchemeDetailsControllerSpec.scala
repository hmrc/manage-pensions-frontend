/*
 * Copyright 2019 HM Revenue & Customs
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

import config.FeatureSwitchManagementService
import connectors._
import controllers.actions.{DataRetrievalAction, _}
import handlers.ErrorHandler
import identifiers.{SchemeNameId, SchemeSrnId}
import models._
import org.mockito.Matchers
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers.{contentAsString, _}
import testhelpers.CommonBuilders._
import utils.UserAnswers
import viewmodels.AssociatedPsa
import views.html.schemeDetails

import scala.concurrent.Future

class SchemeDetailsControllerSpec extends ControllerSpecBase {

  import SchemeDetailsControllerSpec._

  override lazy val app: Application = new GuiceApplicationBuilder().configure(
    "features.work-package-one-enabled" -> true
  ).build()

  def featureSwitchManagementService(toggleValue: Boolean): FeatureSwitchManagementService = new FeatureSwitchManagementService {
    override def change(name: String, newValue: Boolean): Boolean = ???

    override def get(name: String): Boolean = toggleValue

    override def reset(name: String): Unit = ???

  }

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData,
                 variationsToggle: Boolean = false): SchemeDetailsController = {
    val eh = new ErrorHandler(frontendAppConfig, messagesApi)
    new SchemeDetailsController(frontendAppConfig,
      messagesApi,
      fakeSchemeDetailsConnector,
      fakeListOfSchemesConnector,
      fakeSchemeLockConnector,
      FakeAuthAction(),
      dataRetrievalAction,
      FakeUserAnswersCacheConnector,
      eh,
      featureSwitchManagementService(variationsToggle)
    )
  }

  def viewAsString(openDate: Option[String] = openDate, administrators: Option[Seq[AssociatedPsa]] = administrators,
                   isSchemeOpen: Boolean = true, SchemeNotLocked: Boolean = true): String =
    schemeDetails(
      frontendAppConfig,
      mockSchemeDetails.name,
      openDate,
      administrators,
      srn,
      isSchemeOpen,
      SchemeNotLocked
    )(fakeRequest, messages).toString()

  "SchemeDetailsController" must {
        "save the srn and then return OK and the correct view for a GET" in {
          reset(fakeSchemeDetailsConnector)
          when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(schemeDetailsWithPsaOnlyResponse))
          when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(listOfSchemesResponse))
          val result = controller().onPageLoad(srn)(fakeRequest)
          status(result) mustBe OK
          contentAsString(result) mustBe viewAsString()
          FakeUserAnswersCacheConnector.verify(SchemeSrnId, srn.id)
        }

    "return OK and the correct view for a GET where administrators a mix of individual and org" in {
      val updatedAdministrators =
        Some(
          Seq(
            AssociatedPsa("partnetship name 2", true),
            AssociatedPsa("Smith A Tony", false)
          )
        )

      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(schemeDetailsWithPsaOnlyResponseMixOfIndividualAndOrg))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(administrators = updatedAdministrators)
    }

    "return OK and call the correct connector method for a GET where administrators a mix of individual and org where variations toggle is switched on" in {

      val desUserAnswers = UserAnswers(Json.obj(
        "schemeStatus" -> "Open",
        SchemeNameId.toString -> mockSchemeDetails.name,
        "psaDetails" -> JsArray(
        Seq(
          Json.obj(
            "id"-> "A0000000",
            "organisationOrPartnershipName" -> "partnetship name 2",
            "relationshipDate" -> "2018-07-01"
          ),
          Json.obj(
            "id"->"A0000001",
            "individual" -> Json.obj(
              "firstName" -> "Tony",
              "middleName" -> "A",
              "lastName" -> "Smith"
            ),
            "relationshipDate" -> "2018-07-01"
          )
        )
      )
      ))

      val updatedAdministrators =
        Some(
          Seq(
            AssociatedPsa("partnetship name 2", true),
            AssociatedPsa("Tony A Smith", false)
          )
        )
      reset(fakeSchemeDetailsConnector, fakeSchemeLockConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetailsVariations(Matchers.eq("A0000000"), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(desUserAnswers)
        )

      when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(Matchers.eq("A0000000"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(VarianceLock))
        )
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller(variationsToggle = true).onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      verify(fakeSchemeDetailsConnector, times(1))
        .getSchemeDetailsVariations(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      contentAsString(result) mustBe viewAsString(administrators = updatedAdministrators, SchemeNotLocked=false)
    }

    "return OK and the correct view for a GET when opened date is not returned by API" in {
      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(schemeDetailsWithPsaOnlyResponse))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesPartialResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(None)
    }

    "return OK and the correct view for a GET when scheme status is not open" in {
      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(schemeDetailsPendingResponse))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(openDate = None, administrators = administratorsCantRemove, isSchemeOpen = false)
    }

    "return NOT_FOUND when PSA data is not returned by API (as we don't know who administers the scheme)" in {
      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(schemeDetailsWithoutPsaResponse))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe NOT_FOUND
    }

    "return NOT_FOUND and the correct not found view when PSA data is returned by API which does not include the currently logged-in PSA" in {
      val psaDetails1 = PsaDetails("A0000001", Some("partnership name no 1"), None, Some("2018-10-01"))
      val psaDetails2 = PsaDetails("A0000002", Some("partnership name no 2"), None, None)
      val psaSchemeDetailsResponseTwoPSAs = PsaSchemeDetails(mockSchemeDetails, None, None, Some(Seq(psaDetails1, psaDetails2)))

      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(psaSchemeDetailsResponseTwoPSAs))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe NOT_FOUND
      contentAsString(result).contains(messages("messages__pageNotFound404__heading")) mustBe true
    }
  }
}

private object SchemeDetailsControllerSpec extends MockitoSugar {

  val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  val fakeListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  val fakeSchemeLockConnector: PensionSchemeVarianceLockConnector = mock[PensionSchemeVarianceLockConnector]
  val schemeName = "Test Scheme Name"

  val administrators =
    Some(
      Seq(
        AssociatedPsa("Taylor Middle Rayon", true),
        AssociatedPsa("Smith A Tony", false)
      )
    )

  val administratorsCantRemove =
    Some(
      Seq(
        AssociatedPsa("Taylor Middle Rayon", false),
        AssociatedPsa("Smith A Tony", false)
      )
    )

  val openDate = Some("10 October 2012")
  val srn = SchemeReferenceNumber("S1000000456")
}
