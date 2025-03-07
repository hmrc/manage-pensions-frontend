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

package controllers.psa.remove

import base.SpecBase
import connectors.FakeUserAnswersCacheConnector
import connectors.admin.MinimalConnector
import connectors.scheme.SchemeDetailsConnector
import controllers.actions._
import controllers.psa.remove.routes._
import handlers.ErrorHandler
import identifiers.AssociatedDateId
import identifiers.invitations.{PSTRId, SchemeNameId}
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class RemovePsaControllerSpec extends SpecBase with MockitoSugar {

  import RemovePsaControllerSpec._

  val srn: SchemeReferenceNumber = SchemeReferenceNumber("AB123456C")

  def fakeMinimalPsaConnector(psaMinimalSubscription: MinimalPSAPSP = psaMinimalSubscription): MinimalConnector = new MinimalConnector {
    override def getMinimalPsaDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] =
      Future.successful(psaMinimalSubscription)

    override def getPsaNameFromPsaID()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
      Future.successful(None)

    override def getMinimalPspDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinimalPSAPSP] = ???

    override def getNameFromPspID()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = ???

    override def getEmailInvitation(id: String, idType: String, name: String, srn: SchemeReferenceNumber)
                                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = ???
  }

  private val fakePsaSchemeAuthAction = new FakePsaSchemeAuthAction(app.injector.instanceOf[SchemeDetailsConnector], app.injector.instanceOf[ErrorHandler])

  val userAnswersJson: String =
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

  val userAnswersJsonWithoutPstr: String =
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

  val userAnswersJsonWithoutSchemeName: String =
    s"""{
         "benefits": "opt1",
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

  def fakeSchemeDetailsConnector(json: String = userAnswersJson): SchemeDetailsConnector =
    new SchemeDetailsConnector {

      override def getSchemeDetails(psaId: String,
                                    idNumber: String,
                                    schemeIdType: String
                                   )(implicit hc: HeaderCarrier,
                                     ec: ExecutionContext): Future[UserAnswers] = {

        Future.apply(UserAnswers(Json.parse(json)))(ec)
      }

      override def getPspSchemeDetails(pspId: String, srn: String)
                                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] = ???

      override def getSchemeDetailsRefresh(psaId: String,
                                           idNumber: String,
                                           schemeIdType: String)
                                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = ???

      override def isPsaAssociated(psaOrPspId: String, idType: String, srn: String)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] =
        Future.successful(Some(true))

      override def getPsaInvitationInfo(idNumber: String, schemeIdType: String)
                                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaInvitationInfoResponse] = ???

    }

  def controller(dataRetrievalAction: DataRetrievalAction = data,
                 psaMinimalDetails: MinimalPSAPSP = psaMinimalSubscription,
                 schemeDetailsConnector: SchemeDetailsConnector) =
    new RemovePsaController(FakeAuthAction, dataRetrievalAction,
      new DataRequiredActionImpl,
      schemeDetailsConnector,
      FakeUserAnswersCacheConnector,
      fakeMinimalPsaConnector(psaMinimalDetails),
      frontendAppConfig,
      controllerComponents,
      fakePsaSchemeAuthAction
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
      when(sdc.getSchemeDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(ua))

      val result = controller(schemeDetailsConnector = sdc).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(ConfirmRemovePsaController.onPageLoad(srn).url)
    }


    "redirect to unable to remove psa page if PSA is suspended" in {

      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = true),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(CanNotBeRemovedController.onPageLoadWhereSuspended().url)
    }

    "redirect to update contact address page if PSA has RLS flag set and deceased flag is false" in {

      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(rlsFlag = true),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(frontendAppConfig.psaUpdateContactDetailsUrl)
    }

    "redirect to contact hmrc page if PSA has both deceased and RLS flags set" in {

      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(rlsFlag = true, deceasedFlag = true),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ContactHMRCController.onPageLoad().url)
    }

    "redirect to contact HMRC page if PSA has deceased flag set" in {

      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(deceasedFlag = true),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ContactHMRCController.onPageLoad().url)
    }

    "redirect to session expired page if no srn in userAnswers" in {

      val result = controller(UserAnswers().dataRetrievalAction,
        psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad.url)
    }

    "save scheme name pstr, then redirect to remove as scheme administrator page if PSA is not suspended" in {
      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(ConfirmRemovePsaController.onPageLoad(srn).url)

      FakeUserAnswersCacheConnector.verify(SchemeNameId, "Test Scheme name")
      FakeUserAnswersCacheConnector.verify(PSTRId, "test pstr")
      FakeUserAnswersCacheConnector.verify(AssociatedDateId, LocalDate.parse("2018-10-01"))
    }

    "redirect to PSTR missing page if PSTR not available" in {

      val result = controller(schemeDetailsConnector = fakeSchemeDetailsConnector(userAnswersJsonWithoutPstr)).
        onPageLoad(srn)(fakeRequest)

      redirectLocation(result) mustBe Some(controllers.psa.remove.routes.MissingInfoController.onPageLoadPstr().url)
    }

    "redirect to PSA name missing page if PSA name not available" in {

      val result = controller(psaMinimalDetails = psaMinimalSubscription.copy(isPsaSuspended = false,
        organisationName = None, individualDetails = None),
        schemeDetailsConnector = fakeSchemeDetailsConnector()).onPageLoad(srn)(fakeRequest)

      redirectLocation(result) mustBe Some(controllers.psa.remove.routes.MissingInfoController.onPageLoadOther().url)
    }

    "redirect to scheme name missing page if scheme name not available" in {

      val result = controller(schemeDetailsConnector = fakeSchemeDetailsConnector(userAnswersJsonWithoutSchemeName)).
        onPageLoad(srn)(fakeRequest)

      redirectLocation(result) mustBe Some(controllers.psa.remove.routes.MissingInfoController.onPageLoadOther().url)
    }

    "redirect to unauthorised page if user is not authenticated" in {

      val controller = new RemovePsaController(FakeUnAuthorisedAction, data, new DataRequiredActionImpl,
        fakeSchemeDetailsConnector(), FakeUserAnswersCacheConnector,
        fakeMinimalPsaConnector(psaMinimalSubscription.copy(isPsaSuspended = false)), frontendAppConfig,
        controllerComponents,
        fakePsaSchemeAuthAction
      )

      val result = controller.onPageLoad(srn)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
    }
  }
}

object RemovePsaControllerSpec {

  private val userAnswer = UserAnswers().srn("AB123456C")
  private val data = userAnswer.dataRetrievalAction

  private val psaMinimalSubscription = MinimalPSAPSP("test@test.com", isPsaSuspended = false, None, Some(IndividualDetails("First", Some("Middle"), "Last")),
    rlsFlag = false, deceasedFlag = false)
}


