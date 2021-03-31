/*
 * Copyright 2021 HM Revenue & Customs
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

import audit._
import models.{Sent, _}
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.{BeforeAndAfterEach, AsyncWordSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}

import scala.concurrent.Future

class EmailResponseControllerSpec extends AsyncWordSpec with MustMatchers with MockitoSugar with BeforeAndAfterEach {

  import EmailResponseControllerSpec._

  private val mockAuditService = mock[AuditService]
  private val mockAuthConnector = mock[AuthConnector]

  private val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false).
    overrides(Seq(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuditService].toInstance(mockAuditService)
    )).build()

  private val injector = application.injector
  private val controller = injector.instanceOf[EmailResponseController]
  private val encryptedPsaId = injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(psa)).value
  private val encryptedPspId = injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(psp)).value
  private val encryptedPstr = injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(pstr)).value
  private val encryptedEmail = injector.instanceOf[ApplicationCrypto].QueryParameterCrypto.encrypt(PlainText(email)).value

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuditService, mockAuthConnector)
    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
      .thenReturn(Future.successful(enrolments))
  }

  "retrieveStatusForPSPDeauthorisation" must {
    "respond OK when given EmailEvents" in {
      val result = controller
        .retrieveStatusForPSPDeauthorisation(encryptedPsaId, encryptedPspId, encryptedPstr, encryptedEmail)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result) mustBe OK
      verify(mockAuditService, times(4)).sendEvent(eventCaptor.capture())(any(), any())
      eventCaptor.getValue mustEqual PSPDeauthorisationEmailAuditEvent(psa, psp, pstr, email, Complained)
    }

    "respond with BAD_REQUEST when not given EmailEvents" in {
      val result = controller
        .retrieveStatusForPSPDeauthorisation(encryptedPsaId, encryptedPspId, encryptedPstr, encryptedEmail)(fakeRequest.withBody(Json.obj("name" -> "invalid")))

      verify(mockAuditService, never()).sendEvent(any())(any(), any())
      status(result) mustBe BAD_REQUEST
    }
  }

  "retrieveStatusForPSPSelfDeauthorisation" must {
    "respond OK when given EmailEvents" in {
      val result = controller
        .retrieveStatusForPSPSelfDeauthorisation(encryptedPspId, encryptedPstr, encryptedEmail)(fakeRequest.withBody(Json.toJson(emailEvents)))

      status(result) mustBe OK
      verify(mockAuditService, times(4)).sendEvent(eventCaptor.capture())(any(), any())
      eventCaptor.getValue mustEqual PSPSelfDeauthorisationEmailAuditEvent(psp, pstr, email, Complained)
    }

    "respond with BAD_REQUEST when not given EmailEvents" in {
      val result = controller
        .retrieveStatusForPSPSelfDeauthorisation(encryptedPspId, encryptedPstr, encryptedEmail)(fakeRequest.withBody(Json.obj("name" -> "invalid")))

      verify(mockAuditService, never()).sendEvent(any())(any(), any())
      status(result) mustBe BAD_REQUEST
    }
  }
}

object EmailResponseControllerSpec {
  private val psa = "A8654321"
  private val psp = "27654321"
  private val pstr = "121212"
  private val email = "test@test.com"
  private val fakeRequest = FakeRequest("", "")
  private val enrolments = Enrolments(Set(
    Enrolment("HMRC-PODS-ORG", Seq(
      EnrolmentIdentifier("PSPID", "A0000000")
    ), "Activated", None)
  ))
  private val eventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])
  private val emailEvents = EmailEvents(Seq(EmailEvent(Sent, DateTime.now()), EmailEvent(Delivered, DateTime.now()),
    EmailEvent(PermanentBounce, DateTime.now()), EmailEvent(Opened, DateTime.now()), EmailEvent(Complained, DateTime.now())))
}
