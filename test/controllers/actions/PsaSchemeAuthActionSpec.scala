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

package controllers.actions

import base.SpecBase
import connectors.scheme.SchemeDetailsConnector
import handlers.ErrorHandler
import models.{AuthEntity, Individual, SchemeReferenceNumber}
import models.requests.OptionalDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.domain.PsaId
import utils.UserAnswers

import scala.concurrent.Future

class PsaSchemeAuthActionSpec
  extends SpecBase with MockitoSugar with BeforeAndAfterAll with ScalaFutures {

  private val errorHandler = mock[ErrorHandler]
  private val schemeDetailsConnector = mock[SchemeDetailsConnector]
  private val action = new PsaSchemeAuthAction(schemeDetailsConnector, errorHandler)
  private val notFoundTemplateResult = Html("")

  override def beforeAll(): Unit = {
    when(errorHandler.notFoundTemplate(any())).thenReturn(notFoundTemplateResult)
  }

  override def afterAll(): Unit = {
    reset(errorHandler)
    reset(schemeDetailsConnector)
  }


  "PsaSchemeAuthActionSpec" must {

    "return not found if PSAId not found" in {
      val request = OptionalDataRequest(fakeRequest, "", None, None, None , Individual, AuthEntity.PSA)
      val result = action.apply((SchemeReferenceNumber("AB123456C"))).invokeBlock(request, { x: OptionalDataRequest[_] => Future.successful(Ok("")) })
      status(result) mustBe NOT_FOUND
    }

    "return not found if getSchemeDetails fails" in {
      when(schemeDetailsConnector.isPsaAssociated(any(), any(), any())(any(), any())).thenReturn(Future.failed(new RuntimeException("")))
      val request = OptionalDataRequest(fakeRequest, "", None, Some(PsaId("A0000000")), None , Individual, AuthEntity.PSA)
      val result = action.apply((SchemeReferenceNumber("AB123456C"))).invokeBlock(request, { x:OptionalDataRequest[_] => Future.successful(Ok("")) })
      status(result) mustBe NOT_FOUND
    }

    "return not found if current psaId is missing from list of scheme admins" in {
      when(schemeDetailsConnector.isPsaAssociated(any(), any(), any())(any(), any())).thenReturn(Future.successful(Some(false)))

      val request = OptionalDataRequest(fakeRequest, "", None, Some(PsaId("A0000001")), None , Individual, AuthEntity.PSA)
      val result = action.apply((SchemeReferenceNumber("AB123456C"))).invokeBlock(request, { x:OptionalDataRequest[_] => Future.successful(Ok("")) })
      status(result) mustBe NOT_FOUND
    }

    "return ok after making an API call and ensuring that PSAId is authorised" in {
      when(schemeDetailsConnector.isPsaAssociated(any(), any(), any())(any(), any())).thenReturn(Future.successful(Some(true)))

      val request = OptionalDataRequest(fakeRequest, "", None, Some(PsaId("A0000000")), None , Individual, AuthEntity.PSA)
      val result = action.apply((SchemeReferenceNumber("AB123456C"))).invokeBlock(request, { x:OptionalDataRequest[_] => Future.successful(Ok("")) })
      status(result) mustBe OK
    }
  }
}
