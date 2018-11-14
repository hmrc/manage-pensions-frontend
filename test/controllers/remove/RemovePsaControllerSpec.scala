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

package controllers.remove

import base.SpecBase
import connectors.{FakeUserAnswersCacheConnector, MinimalPsaConnector, SchemeDetailsConnector}
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeUnAuthorisedAction}
import identifiers.invitations.{PSANameId, SchemeNameId}
import models._
import play.api.test.Helpers._
import testhelpers.CommonBuilders
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class RemovePsaControllerSpec extends SpecBase {

  import RemovePsaControllerSpec._

  def fakeMinimalPsaConnector(isSuspended: Boolean): MinimalPsaConnector = new MinimalPsaConnector {
    override def getMinimalPsaDetails(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSA] =
      Future.successful(psaMinimalSubscription.copy(isPsaSuspended = isSuspended))
  }

  def fakeSchemeDetailsConnector: SchemeDetailsConnector = new SchemeDetailsConnector {
    override def getSchemeDetails(schemeIdType: String, idNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaSchemeDetails] =
      Future.successful(CommonBuilders.psaSchemeDetailsResponse)
  }

  def controller(isSuspended: Boolean, dataRetrievalAction: DataRetrievalAction = data) = new RemovePsaController(FakeAuthAction(), dataRetrievalAction,
    new DataRequiredActionImpl, fakeSchemeDetailsConnector,
    FakeUserAnswersCacheConnector, fakeMinimalPsaConnector(isSuspended))


  "RemovePsaController calling onPageLoad" must {

    "redirect to unable to remove psa page if PSASuspension is true" in {

      val result = controller(isSuspended = true).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.remove.routes.CanNotBeRemovedController.onPageLoad().url)
    }

    "redirect to session expired page if no srn in userAnswers" in {

      val result = controller(isSuspended = false, UserAnswers().dataRetrievalAction).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

    "save scheme name and psa name, then redirect to remove as scheme administrator page if PSASuspension is false" in {
      val result = controller(isSuspended = false).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.remove.routes.ConfirmRemovePsaController.onPageLoad().url)

      FakeUserAnswersCacheConnector.verify(SchemeNameId, schemeName)
      FakeUserAnswersCacheConnector.verify(PSANameId, psaMinimalSubscription.individualDetails.map(_.fullName).getOrElse(""))
    }

    "redirect to unauthorised page if user is not authenticated" in {

      val controller = new RemovePsaController(FakeUnAuthorisedAction(), data, new DataRequiredActionImpl,
        fakeSchemeDetailsConnector, FakeUserAnswersCacheConnector, fakeMinimalPsaConnector(isSuspended = false))

      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }
  }
}

object RemovePsaControllerSpec {
  private val email = "test@test.com"
  private val srn = "S9000000000"
  private val schemeName = "Benefits Scheme"
  private val userAnswer = UserAnswers().srn(srn)
  private val data = userAnswer.dataRetrievalAction

  private val psaMinimalSubscription = MinimalPSA(email, false, None, Some(IndividualDetails("First", Some("Middle"), "Last")))
}


