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

package controllers.invitations

import base.JsonFileReader
import base.SpecBase
import connectors.admin.MinimalConnector
import connectors.FakeUserAnswersCacheConnector
import connectors.scheme.SchemeDetailsConnector
import controllers.actions.FakeAuthAction
import controllers.actions.FakeUnAuthorisedAction
import identifiers.MinimalSchemeDetailId
import models._
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class InviteControllerSpec extends SpecBase {

  import InviteControllerSpec._



  "InviteController calling onPageLoad(srn)" must {

    "redirect to you cannot send an invite page if PSASuspension is true" in {

      val result = controller(isSuspended = true).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.invitations.routes.YouCannotSendAnInviteController.onPageLoad().url)

      FakeUserAnswersCacheConnector.verifyNot(MinimalSchemeDetailId)
    }

    "save minimal scheme details and then redirect to psa name page if PSASuspension is false" in {
      val result = controller(isSuspended = false).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.invitations.routes.WhatYouWillNeedController.onPageLoad().url)

      FakeUserAnswersCacheConnector.verify(MinimalSchemeDetailId, MinimalSchemeDetail(srn, Some(pstr), schemeName))
    }

    "redirect to unauthorised page if user is not authenticated" in {

      val controller = new InviteController(FakeUnAuthorisedAction, fakeSchemeDetailsConnector,
        FakeUserAnswersCacheConnector, fakeMinimalPsaConnector(isSuspended = false), stubMessagesControllerComponents())

      val result = controller.onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }
  }
}

object InviteControllerSpec extends SpecBase with JsonFileReader {
  private val email = "test@test.com"
  val srn = "S9000000000"
  val pstr = "24000001IN"
  val schemeName = "Open Single Trust Scheme with Indiv Establisher and Trustees"

  private val psaMinimalSubscription = MinimalPSAPSP(email, false, None, Some(IndividualDetails("First", Some("Middle"), "Last")))
  private val mockAuthAction = FakeAuthAction


  val config = injector.instanceOf[Configuration]

  def fakeMinimalPsaConnector(isSuspended: Boolean): MinimalConnector = new MinimalConnector {
    override def getMinimalPsaDetails(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] =
      Future.successful(psaMinimalSubscription.copy(isPsaSuspended = isSuspended))

    override def getPsaNameFromPsaID(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
      Future.successful(None)

    override def getMinimalPspDetails(pspId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] = ???

    override def getNameFromPspID(pspId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = ???
  }

  def fakeSchemeDetailsConnector: SchemeDetailsConnector = new SchemeDetailsConnector {

    override def getSchemeDetails(psaId: String,
                                  idNumber: String,
                                  schemeIdType: String
                                 )(implicit hc: HeaderCarrier,
                                                              ec: ExecutionContext): Future[UserAnswers] =
      Future.successful(UserAnswers(readJsonFromFile("/data/validSchemeDetailsUserAnswers.json")))
  }

  def controller(isSuspended: Boolean) = new InviteController(mockAuthAction, fakeSchemeDetailsConnector,
    FakeUserAnswersCacheConnector, fakeMinimalPsaConnector(isSuspended), stubMessagesControllerComponents())
}
