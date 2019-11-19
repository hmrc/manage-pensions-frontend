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

import java.time.LocalDate

import base.SpecBase
import connectors.{FakeUserAnswersCacheConnector, MinimalPsaConnector, SchemeDetailsConnector}
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeUnAuthorisedAction}
import identifiers.AssociatedDateId
import identifiers.invitations.{PSTRId, SchemeNameId}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class RemovePsaControllerSpec extends SpecBase with MockitoSugar {

  import RemovePsaControllerSpec._

  def fakeMinimalPsaConnector(psaMinimalSubscription: MinimalPSA = psaMinimalSubscription): MinimalPsaConnector = new MinimalPsaConnector {
    override def getMinimalPsaDetails(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSA] =
      Future.successful(psaMinimalSubscription)

    override def getPsaNameFromPsaID(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
      Future.successful(None)
  }

  val userAnswersJson =
    s"""{
                       "benefits": "opt1",
                       "schemeName": "Test Scheme name",
                       "schemeType": {
                         "schemeTypeDetails": "test scheme name",
                         "name": "master"
                       },
                       "psaDetails" :[
                         {
                         "id":"A0000000",
                         "individual":{
                             "firstName": "Taylor",
                             "middleName": "Middle",
                             "lastName": "Rayon"
                           },
                           "organisationOrPartnershipName": "partnetship name",
                           "relationshipDate": "2018-10-01"
                         }
                       ],
                       "schemeStatus" : "Pending",
                       "pstr" : "test pstr",
                       "isAboutBenefitsAndInsuranceComplete": true,
                       "isAboutMembersComplete": true,
                       "isBeforeYouStartComplete": true
                     }
                     """.stripMargin

  val userAnswersJsonWithoutPstr =
    s"""{
                       "benefits": "opt1",
                       "schemeName": "Test Scheme name",
                       "schemeType": {
                         "schemeTypeDetails": "test scheme name",
                         "name": "master"
                       },
                       "psaDetails" :[
                         {
                         "id":"A0000000",
                         "individual":{
                             "firstName": "Taylor",
                             "middleName": "Middle",
                             "lastName": "Rayon"
                           },
                           "organisationOrPartnershipName": "partnetship name",
                           "relationshipDate": "2018-10-01"
                         }
                       ],
                       "schemeStatus" : "Pending",
                       "isAboutBenefitsAndInsuranceComplete": true,
                       "isAboutMembersComplete": true,
                       "isBeforeYouStartComplete": true
                     }
                     """.stripMargin

  def fakeSchemeDetailsConnector(json: String = userAnswersJson): SchemeDetailsConnector =
    new SchemeDetailsConnector {

      override def getSchemeDetails(psaId: String,
                                    schemeIdType: String,
                                    idNumber: String)(implicit hc: HeaderCarrier,
                                                                ec: ExecutionContext): Future[UserAnswers] = {

        Future(UserAnswers(Json.parse(json)))
      }
    }

  def controller(dataRetrievalAction: DataRetrievalAction = data,
                 psaMinimalDetails: MinimalPSA = psaMinimalSubscription,
                 schemeDetailsConnector:SchemeDetailsConnector) =
    new RemovePsaController(FakeAuthAction(), dataRetrievalAction,
      new DataRequiredActionImpl,
      schemeDetailsConnector,
      FakeUserAnswersCacheConnector,
      fakeMinimalPsaConnector(psaMinimalDetails),
      frontendAppConfig,
      stubMessagesControllerComponents()
    )


  "RemovePsaController calling onPageLoad" must {

    "redirect correctly" in {
      import identifiers.SchemeNameId
      val schemeName = "test scheme"
      val pstr = "pstr"
      val uaJson = Json.obj(
        SchemeNameId.toString -> schemeName,
        PSTRId.toString -> pstr
      )
      val ua = UserAnswers(uaJson)
      val sdc = mock[SchemeDetailsConnector]
      when(sdc.getSchemeDetails(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(ua))

      val result = controller(schemeDetailsConnector = sdc).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.remove.routes.ConfirmRemovePsaController.onPageLoad().url)
    }


    "redirect to unable to remove psa page if PSA is suspended" in {

      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = true),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.remove.routes.CanNotBeRemovedController.onPageLoadWhereSuspended().url)
    }

    "redirect to session expired page if no srn in userAnswers" in {

      val result = controller(UserAnswers().dataRetrievalAction,
        psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }

    "save scheme name pstr, then redirect to remove as scheme administrator page if PSA is not suspended" in {
      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.remove.routes.ConfirmRemovePsaController.onPageLoad().url)

      FakeUserAnswersCacheConnector.verify(SchemeNameId, "Test Scheme name")
      FakeUserAnswersCacheConnector.verify(PSTRId, "test pstr")
      FakeUserAnswersCacheConnector.verify(AssociatedDateId, LocalDate.parse("2018-10-01"))
    }

    "throw IllegalArgumentException if pstr is not found" in {
      val result = controller(schemeDetailsConnector = fakeSchemeDetailsConnector(userAnswersJsonWithoutPstr)).
        onPageLoad(fakeRequest)

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[IllegalArgumentException]
        e.getMessage mustEqual "PSTR missing while removing PSA"
      }
    }

    "throw IllegalArgumentException if psa name is not found" in {
      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false,
        organisationName = None, individualDetails = None),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(fakeRequest)

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[IllegalArgumentException]
        e.getMessage mustEqual "Organisation or Individual PSA Name missing"
      }
    }

    "redirect to unauthorised page if user is not authenticated" in {

      val controller = new RemovePsaController(FakeUnAuthorisedAction(), data, new DataRequiredActionImpl,
        fakeSchemeDetailsConnector(), FakeUserAnswersCacheConnector,
        fakeMinimalPsaConnector(psaMinimalSubscription.copy(isPsaSuspended = false)), frontendAppConfig,
        stubMessagesControllerComponents()
      )

      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }
  }
}

object RemovePsaControllerSpec {

  private val userAnswer = UserAnswers().srn("S9000000000")
  private val data = userAnswer.dataRetrievalAction

  private val psaMinimalSubscription = MinimalPSA("test@test.com", false, None, Some(IndividualDetails("First", Some("Middle"), "Last")))
}


