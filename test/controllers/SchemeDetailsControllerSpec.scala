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

package controllers

import config.{FeatureSwitchManagementService, FeatureSwitchManagementServiceTestImpl}
import connectors._
import controllers.actions.{DataRetrievalAction, _}
import handlers.ErrorHandler
import identifiers.invitations.PSTRId
import identifiers.{SchemeNameId, SchemeStatusId}
import models._
import org.mockito.Matchers
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers.{contentAsString, _}
import play.api.{Application, Configuration}
import testhelpers.CommonBuilders._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers
import viewmodels.{AFTViewModel, AssociatedPsa, Message}
import views.html.{error_template, error_template_page_not_found, schemeDetails}

import scala.concurrent.Future

class SchemeDetailsControllerSpec extends ControllerSpecBase with BeforeAndAfterEach {

  import SchemeDetailsControllerSpec._

  override lazy val app: Application = new GuiceApplicationBuilder().configure(
  ).build()

  val config: Configuration = injector.instanceOf[Configuration]
  val fakeFeatureSwitch: FeatureSwitchManagementService = new FeatureSwitchManagementServiceTestImpl(config, environment)
  val view: schemeDetails = app.injector.instanceOf[schemeDetails]
  val errorHandlerView: error_template = app.injector.instanceOf[error_template]
  val errorHandlerNotFoundView: error_template_page_not_found = app.injector.instanceOf[error_template_page_not_found]
  val eh = new ErrorHandler(frontendAppConfig, messagesApi, errorHandlerView, errorHandlerNotFoundView)

  private val aftConnector = mock[AFTConnector]

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData): SchemeDetailsController = {
    new SchemeDetailsController(frontendAppConfig,
      messagesApi,
      fakeSchemeDetailsConnector,
      fakeListOfSchemesConnector,
      fakeSchemeLockConnector,
      FakeAuthAction(),
      dataRetrievalAction,
      FakeUserAnswersCacheConnector,
      eh,
      fakeFeatureSwitch,
      fakeMinimalPsaConnector,
      stubMessagesControllerComponents(),
      aftConnector,
      view
    )
  }

  def viewAsString(openDate: Option[String] = openDate, administrators: Option[Seq[AssociatedPsa]] = administrators,
                   isSchemeOpen: Boolean = true, displayChangeLink: Boolean = false, lockingPsa: Option[String] = None,
                   optionAFTViewModel: Option[AFTViewModel] = None
                  ): String =
    view(
      schemeName1,
      pstr,
      openDate,
      administrators,
      srn,
      isSchemeOpen,
      displayChangeLink,
      lockingPsa,
      optionAFTViewModel
    )(fakeRequest, messages).toString()

  override def beforeEach(): Unit = {
    reset(fakeSchemeDetailsConnector, fakeListOfSchemesConnector, fakeSchemeLockConnector, aftConnector)
  }

  "SchemeDetailsController" must {

    "return OK and call the correct connector method for a GET where administrators a mix of individual and org" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.eq("A0000000"), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(desUserAnswers))
      when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(Matchers.eq("A0000000"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(VarianceLock)))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      when(aftConnector.getListOfVersions(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      verify(fakeSchemeDetailsConnector, times(1))
        .getSchemeDetails(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
      contentAsString(result) mustBe viewAsString(administrators = updatedAdministrators, displayChangeLink = true)
    }

    "return OK and pass in the correct aft view model" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.eq("A0000000"), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(desUserAnswers))
      when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(Matchers.eq("A0000000"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(VarianceLock)))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      when(aftConnector.getListOfVersions(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(Seq(1))))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(administrators = updatedAdministrators, displayChangeLink = true,
        optionAFTViewModel = Some(
          AFTViewModel(
            Some(Message("messages__schemeDetails__aft_period")),
            Some(Message("messages__schemeDetails__aft_inProgress")),
            Link(
              id = "aftSummaryPageLink",
              url = frontendAppConfig.aftSummaryPageUrl.format(srn.id),
              linkText = Message("messages__schemeDetails__aft_view"))
          )
        )
      )
    }

    "return OK and the correct view for a GET when scheme is locked by another PSA" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.eq("A0000000"), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(desUserAnswers))
      when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(Matchers.eq("A0000000"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(SchemeLock)))
      when(fakeSchemeLockConnector.getLockByScheme(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(SchemeVariance("A0000001", "S1000000456"))))
      when(fakeMinimalPsaConnector.getPsaNameFromPsaID(Matchers.eq("A0000001"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some("Locky Lockhart")))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      when(aftConnector.getListOfVersions(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(
        administrators = updatedAdministrators,
        displayChangeLink = false,
        lockingPsa = Some("Locky Lockhart"))
    }

    "return OK and the correct view with View only link for a GET when scheme status is not open" in {
      val updatedAdministrators = Some(Seq(AssociatedPsa("partnetship name 2", false), AssociatedPsa("Tony A Smith", false)))
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.eq("A0000000"), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(desUserAnswers.set(SchemeStatusId)("Pending").asOpt.value))
      when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(Matchers.eq("A0000000"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(VarianceLock)))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      when(aftConnector.getListOfVersions(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(openDate = None, administrators = updatedAdministrators, isSchemeOpen = false)
    }

    "return NOT_FOUND when PSA data is not returned by API (as we don't know who administers the scheme)" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.eq("A0000000"), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(UserAnswers(Json.obj("psaDetails" -> JsArray()))))
      when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(Matchers.eq("A0000000"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(VarianceLock)))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      when(aftConnector.getListOfVersions(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe NOT_FOUND
    }

    "return NOT_FOUND and the correct not found view when PSA data is returned by API which does not include the currently logged-in PSA" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.eq("A0000000"), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(UserAnswers(Json.obj("schemeStatus" -> "Open",
          SchemeNameId.toString -> schemeName1,
          "psaDetails" -> JsArray(Seq(
            Json.obj(
              "id" -> "A0000007",
              "organisationOrPartnershipName" -> "partnetship name 2",
              "relationshipDate" -> "2018-07-01"
            )))))))
      when(fakeListOfSchemesConnector.getListOfSchemes(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(Matchers.eq("A0000000"), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(VarianceLock)))
      when(aftConnector.getListOfVersions(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

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
  val fakeMinimalPsaConnector: MinimalPsaConnector = mock[MinimalPsaConnector]
  val schemeName = "Test Scheme Name"
  val schemeName1 = "Benefits Scheme"
  val pstr = Some("10000678RE")

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

  val desUserAnswers = UserAnswers(Json.obj(
    PSTRId.toString -> pstr.get,
    "schemeStatus" -> "Open",
    SchemeNameId.toString -> schemeName1,
    "psaDetails" -> JsArray(
      Seq(
        Json.obj(
          "id" -> "A0000000",
          "organisationOrPartnershipName" -> "partnetship name 2",
          "relationshipDate" -> "2018-07-01"
        ),
        Json.obj(
          "id" -> "A0000001",
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
}
