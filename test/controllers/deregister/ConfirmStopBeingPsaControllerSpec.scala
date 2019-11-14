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

package controllers.deregister

import audit.StubSuccessfulAuditService
import connectors._
import controllers.ControllerSpecBase
import controllers.actions._
import forms.deregister.ConfirmStopBeingPsaFormProvider
import identifiers.PSANameId
import models.{IndividualDetails, MinimalPSA}
import org.scalatest.concurrent.ScalaFutures
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, BodyParsers, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import views.html.deregister.confirmStopBeingPsa

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ConfirmStopBeingPsaControllerSpec extends ControllerSpecBase with ScalaFutures {

  import ConfirmStopBeingPsaControllerSpec._

  "ConfirmStopBeingPsaController" must {

    "return to session expired if psaName is not present" in {
      val result = controller()(hc).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

    "return OK and the correct view for a GET and ensure audit service is successfully called" in {
      val psa = PsaId("A1234567")
      val user = "Fred"
      val request = fakeRequest.withJsonBody(Json.obj(
        "userId" -> user,
        "psaId" -> psa)
      )

      val result = controller(minimalPsaDetailsIndividual)(hc).onPageLoad()(request)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return to you cannot stop being a psa page if psa suspended flag is set in minimal details" in {
      val result = controller(minimalPsaDetailsNoneSuspended)(hc).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.deregister.routes.UnableToStopBeingPsaController.onPageLoad().url)
    }

    "return to session expired if psaName is not present for Post" in {
      val result = controller()(hc).onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

    "should display the errors if no selection made" in {

      val result = controller(minimalPsaDetailsIndividual)(hc).onSubmit()(fakeRequest)

      status(result) mustBe BAD_REQUEST
    }

    "redirect to the next page on a successful POST when selected true" in {

      val result = controller(minimalPsaDetailsIndividual)(hc).onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.deregister.routes.SuccessfulDeregistrationController.onPageLoad().url)
    }

    "redirect to the next page and clear the user cache on a successful POST when selected true" in {

      val result = controller(minimalPsaDetailsIndividual)(hc).onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      FakeUserAnswersCacheConnector.verifyAllDataRemoved()
    }

    "redirect to the next page on a successful POST when selected false" in {

      val result = controller(minimalPsaDetailsIndividual)(hc).onSubmit()(postRequestCancel)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(psaDetailsUrl)
    }
  }

}

object ConfirmStopBeingPsaControllerSpec extends ControllerSpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val fakeAuditService = new StubSuccessfulAuditService()

  private def psaDetailsUrl = frontendAppConfig.registeredPsaDetailsUrl

  private val formProvider = new ConfirmStopBeingPsaFormProvider
  private val form: Form[Boolean] = formProvider()

  private val postRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest().withFormUrlEncodedBody(("value", "true"))

  private val postRequestCancel: FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest().withFormUrlEncodedBody(("value", "false"))

  private def testData = new FakeDataRetrievalAction(Some(Json.obj(PSANameId.toString -> "psaName")))


  private def fakeTaxEnrolmentsConnector: TaxEnrolmentsConnector = new TaxEnrolmentsConnector {
    override def deEnrol(groupId: String, psaId: String, userId: String)(
      implicit hc: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[HttpResponse] = Future.successful(HttpResponse(NO_CONTENT))
  }

  private def fakeDeregistrationConnector: DeregistrationConnector = new DeregistrationConnector {
    override def stopBeingPSA(psaId: String)(
      implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = Future.successful(HttpResponse(NO_CONTENT))

    override def canDeRegister(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = Future.successful(true)
  }

  private def fakeMinimalPsaConnector(minimalPsaDetailsIndividual: MinimalPSA): MinimalPsaConnector = new MinimalPsaConnector {
    override def getMinimalPsaDetails(psaId: String)(
      implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSA] = Future.successful(minimalPsaDetailsIndividual)

    override def getPsaNameFromPsaID(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
      Future.successful(None)
  }

  private val minimalPsaDetailsIndividual = MinimalPSA("test@test.com", isPsaSuspended = false, None, Some(IndividualDetails("John", Some("Doe"), "Doe")))
  private val minimalPsaDetailsNone = MinimalPSA("test@test.com", isPsaSuspended = false, None, None)
  private val minimalPsaDetailsNoneSuspended = MinimalPSA("test@test.com", isPsaSuspended = true, None, None)

  private def fakeAllowAccess(minimalPsaConnector: MinimalPsaConnector): AllowAccessForNonSuspendedUsersAction = {
    new AllowAccessForNonSuspendedUsersAction(minimalPsaConnector) {
      def apply(): AllowAccessForNonSuspendedUsersAction = new AllowAccessForNonSuspendedUsersAction(minimalPsaConnector)
    }
  }
  val view = app.injector.instanceOf[confirmStopBeingPsa]

  private def controller(minimalPsaDetails: MinimalPSA = minimalPsaDetailsNone)(implicit hc: HeaderCarrier) = {
    val minimalDetailsConnector = fakeMinimalPsaConnector(minimalPsaDetails)
    new ConfirmStopBeingPsaController(
      frontendAppConfig,
      FakeAuthAction(),
      messagesApi,
      formProvider,
      minimalDetailsConnector,
      fakeDeregistrationConnector,
      fakeTaxEnrolmentsConnector,
      fakeAllowAccess(minimalDetailsConnector),
      FakeUserAnswersCacheConnector,
      stubMessagesControllerComponents(),
      view
    )
  }

  private def viewAsString(): String =
    view(form, "John Doe Doe")(fakeRequest, messages).toString

}


