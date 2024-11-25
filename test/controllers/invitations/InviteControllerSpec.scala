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

package controllers.invitations

import base.JsonFileReader
import config.FrontendAppConfig
import connectors.FakeUserAnswersCacheConnector
import connectors.admin.MinimalConnector
import connectors.scheme.SchemeDetailsConnector
import controllers.ControllerSpecBase
import controllers.actions.{FakeAuthAction, FakeUnAuthorisedAction}
import controllers.invitations.psa.routes._
import controllers.invitations.routes.YouCannotSendAnInviteController
import controllers.routes._
import identifiers.MinimalSchemeDetailId
import models._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class InviteControllerSpec extends ControllerSpecBase {

  import InviteControllerSpec._


  "InviteController calling onPageLoad(srn)" must {

    "redirect to you cannot send an invite page if PSASuspension is true" in {

      val result = controller(isSuspended = true).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(YouCannotSendAnInviteController.onPageLoad(srn).url)

      FakeUserAnswersCacheConnector.verifyNot(MinimalSchemeDetailId)
    }

    "redirect to update contact page if rls flag is true" in {

      when(mockAppConfig.psaUpdateContactDetailsUrl).thenReturn(dummyUrl)

      val result = controller(isSuspended = false, rlsFlag = true).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "redirect to contact hmrc page if both rls flag and deceased flag are true" in {

      when(mockAppConfig.psaUpdateContactDetailsUrl).thenReturn(dummyUrl)

      val result = controller(isSuspended = false, rlsFlag = true, deceasedFlag = true).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(ContactHMRCController.onPageLoad().url)
    }

    "redirect to contact HMRC page if deceased flag is true" in {

      val result = controller(isSuspended = false, deceasedFlag = true).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(ContactHMRCController.onPageLoad().url)
    }

    "save minimal scheme details and then redirect to psa name page if PSASuspension is false" in {
      val result = controller(isSuspended = false).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(WhatYouWillNeedController.onPageLoad(srn).url)

      FakeUserAnswersCacheConnector.verify(MinimalSchemeDetailId, MinimalSchemeDetail(srn, Some(pstr), schemeName))
    }

    "redirect to unauthorised page if user is not authenticated" in {

      val controller = new InviteController(FakeUnAuthorisedAction, fakeSchemeDetailsConnector,
        FakeUserAnswersCacheConnector, fakeMinimalPsaConnector(isSuspended = false), controllerComponents, mockAppConfig, fakePsaSchemeAuthAction, getDataWithPsaName())

      val result = controller.onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(UnauthorisedController.onPageLoad.url)
    }
  }
}

object InviteControllerSpec extends ControllerSpecBase with JsonFileReader with MockitoSugar {
  private val email = "test@test.com"
  val localSrn = "S9000000000"
  val pstr = "24000001IN"
  val schemeName = "Open Single Trust Scheme with Indiv Establisher and Trustees"

  private val psaMinimalSubscription = MinimalPSAPSP(email, isPsaSuspended = false, None, Some(IndividualDetails("First", Some("Middle"), "Last")),
    rlsFlag = false, deceasedFlag = false)
  private val mockAuthAction = FakeAuthAction

  private val dummyUrl = "/url"
  private val mockAppConfig = mock[FrontendAppConfig]

  val config: Configuration = injector.instanceOf[Configuration]

  def fakeMinimalPsaConnector(isSuspended: Boolean, rlsFlag: Boolean = false, deceasedFlag: Boolean = false): MinimalConnector = new MinimalConnector {
    override def getMinimalPsaDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] =
      Future.successful(psaMinimalSubscription.copy(isPsaSuspended = isSuspended, rlsFlag = rlsFlag, deceasedFlag = deceasedFlag))

    override def getPsaNameFromPsaID()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
      Future.successful(None)

    override def getMinimalPspDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] = ???

    override def getNameFromPspID()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = ???

    override def getEmailInvitation(id: String, idType: String, name: String)
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = ???
  }

  def fakeSchemeDetailsConnector: SchemeDetailsConnector = new SchemeDetailsConnector {

    override def getSchemeDetails(psaId: String,
                                  idNumber: String,
                                  schemeIdType: String
                                 )(implicit hc: HeaderCarrier,
                                   ec: ExecutionContext): Future[UserAnswers] =
      Future.successful(UserAnswers(readJsonFromFile("/data/validSchemeDetailsUserAnswers.json")))

    override def isPsaAssociated(psaOrPspId: String, idType: String, srn: String)
                                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] =
                                       Future.successful(Some(true))

    override def getPspSchemeDetails(pspId: String, srn: String)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] = ???

    override def getSchemeDetailsRefresh(psaId: String,
                                         idNumber: String,
                                         schemeIdType: String)
                                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = ???
  }

  def controller(isSuspended: Boolean, rlsFlag: Boolean = false, deceasedFlag: Boolean = false) =
    new InviteController(mockAuthAction, fakeSchemeDetailsConnector,
      FakeUserAnswersCacheConnector, fakeMinimalPsaConnector(isSuspended, rlsFlag, deceasedFlag), controllerComponents, mockAppConfig, fakePsaSchemeAuthAction, getDataWithPsaName())
}
