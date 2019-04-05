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

package controllers.remove

import java.lang

import base.SpecBase
import connectors.{FakeUserAnswersCacheConnector, MinimalPsaConnector, SchemeDetailsConnector}
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeUnAuthorisedAction}
import identifiers.invitations.{PSANameId, PSTRId, SchemeNameId}
import models._
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import testhelpers.CommonBuilders
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class RemovePsaControllerSpec extends SpecBase {

  import RemovePsaControllerSpec._

  def fakeMinimalPsaConnector(psaMinimalSubscription: MinimalPSA = psaMinimalSubscription): MinimalPsaConnector = new MinimalPsaConnector {
    override def getMinimalPsaDetails(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSA] =
      Future.successful(psaMinimalSubscription)
  }

  def fakeSchemeDetailsConnector(psaSchemeDetails: PsaSchemeDetails = psaSchemeDetailsResponse): SchemeDetailsConnector =
    new SchemeDetailsConnector {
      override def getSchemeDetails(psaId: String,
                                    schemeIdType: String,
                                    idNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaSchemeDetails] =
        Future.successful(psaSchemeDetails)
      override def getSchemeDetailsVariations(psaId: String,
                                              schemeIdType: String,
                                              idNumber: String)(implicit hc: HeaderCarrier,
                                                                ec: ExecutionContext): Future[UserAnswers] =
        Future.successful(UserAnswers())

    }

  def controller(dataRetrievalAction: DataRetrievalAction = data, psaSchemeDetails: PsaSchemeDetails = psaSchemeDetailsResponse,
                 psaMinimalDetails: MinimalPSA = psaMinimalSubscription) =
    new RemovePsaController(FakeAuthAction(), dataRetrievalAction,
      new DataRequiredActionImpl, fakeSchemeDetailsConnector(psaSchemeDetails),
      FakeUserAnswersCacheConnector, fakeMinimalPsaConnector(psaMinimalDetails))


  "RemovePsaController calling onPageLoad" must {

    "redirect to unable to remove psa page if PSA is suspended" in {

      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = true)).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.remove.routes.CanNotBeRemovedController.onPageLoadWhereSuspended().url)
    }

    "redirect to session expired page if no srn in userAnswers" in {

      val result = controller(UserAnswers().dataRetrievalAction,
        psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false)).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

    "save scheme name,psa name and pstr, then redirect to remove as scheme administrator page if PSA is not suspended" in {
      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false)).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.remove.routes.ConfirmRemovePsaController.onPageLoad().url)

      FakeUserAnswersCacheConnector.verify(SchemeNameId, schemeDetails.schemeDetails.name)
      FakeUserAnswersCacheConnector.verify(PSANameId, psaMinimalSubscription.individualDetails.map(_.fullName).getOrElse(""))
      FakeUserAnswersCacheConnector.verify(PSTRId, schemeDetails.schemeDetails.pstr.getOrElse(""))
    }

    "throw IllegalArgumentException if pstr is not found" in {
      val schemeDetailsUpdated = psaSchemeDetailsResponse.schemeDetails.copy(pstr = None)
      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false),
        psaSchemeDetails = psaSchemeDetailsResponse.copy(schemeDetails = schemeDetailsUpdated)).
        onPageLoad(fakeRequest)

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[IllegalArgumentException]
        e.getMessage mustEqual "PSTR missing while removing PSA"
      }
    }

    "throw IllegalArgumentException if psa name is not found" in {
      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false,
        organisationName = None, individualDetails = None)).onPageLoad(fakeRequest)

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[IllegalArgumentException]
        e.getMessage mustEqual "Organisation or Individual PSA Name missing"
      }
    }

    "redirect to unauthorised page if user is not authenticated" in {

      val controller = new RemovePsaController(FakeUnAuthorisedAction(), data, new DataRequiredActionImpl,
        fakeSchemeDetailsConnector(), FakeUserAnswersCacheConnector,
        fakeMinimalPsaConnector(psaMinimalSubscription.copy(isPsaSuspended = false)))

      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }
  }
}

object RemovePsaControllerSpec {
  val schemeDetails = CommonBuilders.psaSchemeDetailsResponse
  private val userAnswer = UserAnswers().srn(schemeDetails.schemeDetails.srn.getOrElse(""))
  private val data = userAnswer.dataRetrievalAction
  val psaSchemeDetailsResponse = CommonBuilders.psaSchemeDetailsResponse

  private val psaMinimalSubscription = MinimalPSA("test@test.com", false, None, Some(IndividualDetails("First", Some("Middle"), "Last")))
}


