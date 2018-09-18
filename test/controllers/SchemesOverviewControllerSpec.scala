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

import config.FrontendAppConfig
import connectors.{DataCacheConnector, MicroserviceCacheConnector, MinimalPsaConnector}
import controllers.actions.{DataRetrievalAction, _}
import models.{IndividualDetails, MinimalPSA}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers._
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsString, _}
import views.html.schemesOverview

import scala.concurrent.Future

class SchemesOverviewControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {
 import SchemesOverviewControllerSpec._

  val fakeCacheConnector: DataCacheConnector = mock[MicroserviceCacheConnector]
  val fakePsaMinimalConnector: MinimalPsaConnector = mock[MinimalPsaConnector]

  def getConfig(enabled: Boolean): FrontendAppConfig = {
    val app: Application =
      new GuiceApplicationBuilder()
        .configure(
          "features.work-package-one-enabled" -> enabled
        )
        .build()

    def injector: Injector = app.injector

    injector.instanceOf[FrontendAppConfig]
  }

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData, isWorkPackageOneEnabled: Boolean = true): SchemesOverviewController =
    new SchemesOverviewController(getConfig(isWorkPackageOneEnabled), messagesApi, fakeCacheConnector, fakePsaMinimalConnector, FakeAuthAction(),
      dataRetrievalAction)

  val deleteDate: String = DateTime.now(DateTimeZone.UTC).plusDays(getConfig(true).daysDataSaved).toString(formatter)

  def viewAsString(): String = schemesOverview(
    getConfig(true),
    Some(schemeName),
    Some(lastDate.toString(formatter)),
    Some(deleteDate),
    None
  )(fakeRequest, messages).toString

  def viewAsStringNewScheme(): String = schemesOverview(frontendAppConfig, None, None, None, None)(fakeRequest, messages).toString

  def viewWithPsaName(name: String) = schemesOverview(frontendAppConfig, None, None, None, Some(name))(fakeRequest, messages).toString
  def viewWithPsaNameAndScheme(name: Option[String]) = schemesOverview(frontendAppConfig, Some(schemeName),
    Some(lastDate.toString(formatter)),
    Some(deleteDate), name)(fakeRequest, messages).toString

  override def beforeEach(): Unit = {
    reset(fakeCacheConnector)
    reset(fakePsaMinimalConnector)
    super.beforeEach()
  }

  "SchemesOverview Controller" when {
    "on a GET" must {
      "return OK and the correct view if no scheme has been defined" in {
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(minimalPsaDetailsIndividual))

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaName(expectedName)
      }

      "return OK and the correct view if a scheme has been partially defined" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(Json.obj(
            "schemeDetails" -> Json.obj("schemeName" -> schemeName)))))
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(minimalPsaDetailsIndividual))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))


        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(Some(expectedName))
      }

      "return OK and the correct view with an individual name for an individual Psa and no scheme has been defined" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(None))
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(minimalPsaDetailsIndividual))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaName(expectedName)
      }

      "return OK and the correct view with an individual name with no middle name for an individual Psa and no scheme has been defined" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(None))
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(individualPsaDetailsWithNoMiddleName))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val expectedName = s"${minimalPsaDetailsIndividual.individualDetails.get.firstName} ${minimalPsaDetailsIndividual.individualDetails.get.lastName}"

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaName(expectedName)
      }

      "return OK and the correct view with an individual name with no middle name for an individual Psa and if a scheme has been partially defined" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(Json.obj(
          "schemeDetails" -> Json.obj("schemeName" -> schemeName)))))
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(individualPsaDetailsWithNoMiddleName))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val expectedName = s"${minimalPsaDetailsIndividual.individualDetails.get.firstName} ${minimalPsaDetailsIndividual.individualDetails.get.lastName}"

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(Some(expectedName))
      }

      "return OK and the correct view with an individual name for an individual Psa and scheme has been defined" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(Json.obj(
          "schemeDetails" -> Json.obj("schemeName" -> schemeName)))))
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(minimalPsaDetailsIndividual))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(Some(expectedName))
      }

      "return OK and the correct view with an organisation name for an Organisation Psa" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(Json.obj(
          "schemeDetails" -> Json.obj("schemeName" -> schemeName)))))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(minimalPsaDetailsOrg))
        val expectedName = s"${minimalPsaDetailsOrg.organisationName.get}"

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(Some(expectedName))
      }

      "return OK and the correct view with no individual name and organisation name" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(Json.obj(
          "schemeDetails" -> Json.obj("schemeName" -> schemeName)))))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(
          minimalPsaDetailsOrg.copy(organisationName = None)))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(None)
      }

      "return OK and no name when flag is off" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(Json.obj(
          "schemeDetails" -> Json.obj("schemeName" -> schemeName)))))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val result = controller(isWorkPackageOneEnabled = false).onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(None)
      }
    }

    "on a POST with isWorkPackageOneEnabled flag is on" must {

      "redirect to the cannot start registration page if called without a psa name but psa is suspended" in {
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(true)))
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))

        val result = controller().onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe cannotStartRegistrationUrl.url
      }

      "redirect to the register scheme page if called without psa name but psa is not suspended" in {
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(false)))
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))

        val result = controller().onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe frontendAppConfig.registerSchemeUrl
      }

      "redirect to continue register a scheme page if called with a psa name and psa is not suspended" in {
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(false)))
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(Some(Json.obj(
          "schemeDetails" -> Json.obj("schemeName" -> schemeName)))))

        val result = controller().onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe frontendAppConfig.continueSchemeUrl
      }

      "redirect to cannot start registration page if called with a psa name and psa is suspended" in {
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo("A0000000"))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(true)))
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(Some(Json.obj(
          "schemeDetails" -> Json.obj("schemeName" -> schemeName)))))

        val result = controller().onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe cannotStartRegistrationUrl.url
      }
    }

    "on a POST with isWorkPackageOneEnabled flag is off" must {

      "redirect to the register scheme page if called without psa name" in {
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))

        val result = controller(isWorkPackageOneEnabled = false).onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe frontendAppConfig.registerSchemeUrl
        verify(fakePsaMinimalConnector, never()).getMinimalPsaDetails(any())(any(), any())
      }

      "redirect to the continue register scheme page if called with psa name" in {
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(Some(Json.obj(
          "schemeDetails" -> Json.obj("schemeName" -> schemeName)))))

        val result = controller(isWorkPackageOneEnabled = false).onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe frontendAppConfig.continueSchemeUrl
        verify(fakePsaMinimalConnector, never()).getMinimalPsaDetails(any())(any(), any())
      }
    }
  }
}

object SchemesOverviewControllerSpec {
  val schemeName = "Test Scheme Name"
  private val formatter = DateTimeFormat.forPattern("dd MMMM YYYY")
  val lastDate: DateTime = DateTime.now(DateTimeZone.UTC)
  val timestamp: Long = lastDate.getMillis

  def minimalPsaDetails(psaSuspended: Boolean) = MinimalPSA("test@test.com", psaSuspended, Some("Org Name"), None)
  val minimalPsaDetailsIndividual = MinimalPSA("test@test.com", false, None, Some(IndividualDetails("John", Some("Doe"), "Doe")))
  val individualPsaDetailsWithNoMiddleName = MinimalPSA("test@test.com",false,None,Some(IndividualDetails("John",None,"Doe")))
  val minimalPsaDetailsOrg = MinimalPSA("test@test.com", false, Some("Org Name"), None)
  val expectedName = s"${minimalPsaDetailsIndividual.individualDetails.get.firstName} ${minimalPsaDetailsIndividual.individualDetails.get.middleName.get} ${minimalPsaDetailsIndividual.individualDetails.get.lastName}"
  val cannotStartRegistrationUrl = routes.CannotStartRegistrationController.onPageLoad()
}




