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

import config._
import connectors._
import controllers.actions.{DataRetrievalAction, _}
import models.MinimalPSA
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import views.html.schemesOverview

import scala.concurrent.Future

class SchemesOverviewControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  import SchemesOverviewControllerSpec._

  val fakeCacheConnector: UserAnswersCacheConnector = mock[MicroserviceCacheConnector]
  val fakePsaMinimalConnector: MinimalPsaConnector = mock[MinimalPsaConnector]
  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  private def featureSwitchManagementService(isVariationsEnabled:Boolean):FeatureSwitchManagementService =
    new FeatureSwitchManagementService {
    override def change(name: String, newValue: Boolean): Boolean = ???

    override def get(name: String): Boolean = isVariationsEnabled

    override def reset(name: String): Unit = ???
  }

  private val pensionSchemeVarianceLockConnector = mock[PensionSchemeVarianceLockConnector]

  private val updateConnector = mock[UpdateSchemeCacheConnector]

  private val config = app.injector.instanceOf[Configuration]

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData): SchemesOverviewController =
    new SchemesOverviewController(appConfig, messagesApi, fakeCacheConnector, fakePsaMinimalConnector, FakeAuthAction(),
      dataRetrievalAction,
      pensionSchemeVarianceLockConnector, updateConnector, featureSwitchManagementService(isVariationsEnabled = false))

  val deleteDate: String = DateTime.now(DateTimeZone.UTC).plusDays(appConfig.daysDataSaved).toString(formatter)

  def viewAsString(): String = schemesOverview(
    appConfig,
    Some(schemeName),
    Some(lastDate.toString(formatter)),
    Some(deleteDate),
    None,
    psaId,
    variationSchemeName = None,
    variationDeleteDate = None
  )(fakeRequest, messages).toString

  def viewAsStringNewScheme(): String = schemesOverview(frontendAppConfig, None, None, None, None, psaId,
    variationSchemeName = None,
    variationDeleteDate = None)(fakeRequest, messages).toString

  def viewWithPsaName(name: Option[String] = None): String = schemesOverview(frontendAppConfig, None, None, None, name, psaId,
    variationSchemeName = None,
    variationDeleteDate = None)(fakeRequest, messages).toString

  def viewWithPsaNameAndScheme(name: Option[String]): String = schemesOverview(frontendAppConfig, Some(schemeName),
    Some(lastDate.toString(formatter)),
    Some(deleteDate), name, psaId,
    variationSchemeName = None,
    variationDeleteDate = None)(fakeRequest, messages).toString

  override def beforeEach(): Unit = {
    reset(fakeCacheConnector)
    reset(fakePsaMinimalConnector)
    super.beforeEach()
  }

  "SchemesOverview Controller" when {
    "on a GET" must {
      "return OK and the correct view if no scheme has been defined" in {
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))
        when(fakePsaMinimalConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any()))
          .thenReturn(Future.successful(minimalPsaName))

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaName(Some(expectedName))
      }

      "return OK and the correct view with an individual name for an individual Psa and no scheme has been defined" in {
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))
        when(fakePsaMinimalConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaName))
        when(fakeCacheConnector.lastUpdated(any())(any(), any())).thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaName(Some(expectedName))
      }

      "return OK and the correct view with an individual name with no middle name for an individual Psa and no scheme has been defined" in {
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))
        when(fakePsaMinimalConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any())).thenReturn(Future.successful(individualPsaDetailsWithNoMiddleName))
        when(fakeCacheConnector.lastUpdated(any())(any(), any())).thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaName(Some(expectedNameWithoutMiddleName))
      }

      "return OK and the correct view if a scheme has been partially defined" in {

        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(schemeNameJsonOption))
        when(fakePsaMinimalConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaName))
        when(fakeCacheConnector.lastUpdated(any())(any(), any())).thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(Some(expectedName))
      }

      "return OK and the correct view if a scheme has been partially defined with the old schemeDetails" in {

        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(schemeDetailsJsonOption))
        when(fakePsaMinimalConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaName))
        when(fakeCacheConnector.lastUpdated(any())(any(), any())).thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(Some(expectedName))
      }

      "return OK and the correct view with an individual name with no middle name for an individual Psa and if a scheme has been partially defined" in {
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(schemeNameJsonOption))
        when(fakePsaMinimalConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any())).thenReturn(Future.successful(individualPsaDetailsWithNoMiddleName))
        when(fakeCacheConnector.lastUpdated(any())(any(), any())).thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(Some(expectedNameWithoutMiddleName))
      }

      "return OK and the correct view with an individual name for an individual Psa and scheme has been defined" in {
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(schemeNameJsonOption))
        when(fakePsaMinimalConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaName))
        when(fakeCacheConnector.lastUpdated(any())(any(), any())).thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(Some(expectedName))
      }

      "return OK and the correct view with an organisation name for an Organisation Psa" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(schemeNameJsonOption))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))
        when(fakePsaMinimalConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaOrgName))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(expectedPsaOrgName)
      }

      "return OK and the correct view with no individual name and organisation name" in {
        when(fakeCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(schemeNameJsonOption))
        when(fakeCacheConnector.lastUpdated(any())(any(), any()))
          .thenReturn(Future.successful(Some(Json.parse(timestamp.toString))))

        when(fakePsaMinimalConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any())).thenReturn(Future.successful(
          None))

        val result = controller().onPageLoad(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewWithPsaNameAndScheme(None)
      }
    }
    "on a POST with isWorkPackageOneEnabled flag is on" must {

      "redirect to the cannot start registration page if called without a psa name but psa is suspended" in {
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(true)))
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))

        val result = controller().onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe cannotStartRegistrationUrl.url
      }

      "redirect to the register scheme page if called without psa name but psa is not suspended" in {
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(false)))
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))

        val result = controller().onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe frontendAppConfig.registerSchemeUrl
      }

      "redirect to continue register a scheme page if called with a psa name and psa is not suspended" in {
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(false)))
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(schemeNameJsonOption))

        val result = controller().onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe frontendAppConfig.continueSchemeUrl
      }

      "redirect to cannot start registration page if called with a psa name and psa is suspended" in {
        when(fakePsaMinimalConnector.getMinimalPsaDetails(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(true)))
        when(fakeCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(schemeNameJsonOption))

        val result = controller().onClickCheckIfSchemeCanBeRegistered(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe cannotStartRegistrationUrl.url
      }
    }
  }

  "valid authenticated request" must {

    "redirect to overview page" in {

      running(_.overrides(
        bind[AuthAction].toInstance(FakeAuthAction())
      )) {
        implicit app =>

          val request = FakeRequest("GET", "/manage-pension-schemes")

          route(app, request).foreach { result =>

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe controllers.routes.SchemesOverviewController.onPageLoad().url
          }
      }
    }
  }
}

object SchemesOverviewControllerSpec {
  val schemeName = "Test Scheme Name"
  private val formatter = DateTimeFormat.forPattern("dd MMMM YYYY")
  val lastDate: DateTime = DateTime.now(DateTimeZone.UTC)
  val timestamp: Long = lastDate.getMillis
  private val psaId = "A0000000"

  def minimalPsaDetails(psaSuspended: Boolean) = MinimalPSA("test@test.com", psaSuspended, Some("Org Name"), None)

  val minimalPsaName = Some("John Doe Doe")
  val minimalPsaOrgName = Some("Org Name")
  val expectedPsaOrgName = Some("Org Name")
  val individualPsaDetailsWithNoMiddleName = Some("John Doe")
  val minimalPsaDetailsOrg = MinimalPSA("test@test.com", isPsaSuspended = false, Some("Org Name"), None)
  val expectedName: String = "John Doe Doe"
  val expectedNameWithoutMiddleName: String = "John Doe"
  val cannotStartRegistrationUrl: Call = routes.CannotStartRegistrationController.onPageLoad()

  val schemeNameJsonOption = Some(Json.obj("schemeName" -> schemeName))
  val schemeDetailsJsonOption = Some(Json.obj("schemeDetails" -> Json.obj("schemeName" -> schemeName)))
}




