/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.invitations.psa

import base.JsonFileReader
import connectors.scheme.SchemeDetailsConnector
import connectors.{FakeUserAnswersCacheConnector, InvitationConnector, InvitationsCacheConnector}
import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.invitations.psa.DeclarationFormProvider
import identifiers.invitations.{IsMasterTrustId, IsRacDacId, PSTRId, SchemeNameId}
import identifiers.{SchemeNameId => GetSchemeNameId}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers._
import testhelpers.InvitationBuilder
import utils._
import views.html.invitations.psa.declaration

import scala.concurrent.Future

class DeclarationControllerSpec
  extends ControllerSpecBase
    with MockitoSugar
    with BeforeAndAfterEach
    with JsonFileReader {

  import DeclarationControllerSpec._

  val config: Configuration = injector.instanceOf[Configuration]
  private val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val fakeInvitationCacheConnector = mock[InvitationsCacheConnector]
  private val fakeInvitationConnector = mock[InvitationConnector]
  private val view = injector.instanceOf[declaration]

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) = new DeclarationController(
    messagesApi,
    formProvider,
    FakeAuthAction,
    dataRetrievalAction,
    new DataRequiredActionImpl,
    FakeUserAnswersCacheConnector,
    fakeSchemeDetailsConnector,
    fakeInvitationCacheConnector,
    fakeInvitationConnector,
    new FakeNavigator(onwardRoute),
    controllerComponents,
    view
  )

  override def beforeEach(): Unit = {
    reset(fakeSchemeDetailsConnector)
    reset(fakeInvitationCacheConnector)
    reset(fakeInvitationConnector)
  }

  val schemeDetailsResponse: UserAnswers = UserAnswers(readJsonFromFile("/data/validSchemeDetailsUserAnswers.json"))

  private def viewAsString(form: Form[_] = form, isItMasterTrust: Boolean = isMasterTrust, hasWkAdvisor: Boolean = hasAdviser) =
    view(hasWkAdvisor, isItMasterTrust, form)(fakeRequest, messages).toString

  "Declaration Controller" when {

    "on a GET" must {

      "return OK and the correct view with non rac dac master trust scheme" in {
        when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(schemeDetailsResponse))
        val result = controller(data).onPageLoad()(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
        FakeUserAnswersCacheConnector.verify(SchemeNameId, "Open Single Trust Scheme with Indiv Establisher and Trustees")
        FakeUserAnswersCacheConnector.verify(IsMasterTrustId, true)
        FakeUserAnswersCacheConnector.verify(PSTRId, "24000001IN")
        verify(fakeSchemeDetailsConnector, times(1)).getSchemeDetails(any(), any(), any())(any(), any())
      }

      "return OK and the correct view with racdac scheme" in {
        val uaSchemeDetails = UserAnswers().set(IsRacDacId)(true).flatMap(_.set(GetSchemeNameId)("rac dac scheme")).flatMap(_.set(PSTRId)(pstr)).asOpt.get
        val data = new FakeDataRetrievalAction(Some(UserAnswers().
          haveWorkingKnowledge(true).srn(srn).pstr(pstr).json))

        when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(uaSchemeDetails))
        val result = controller(data).onPageLoad()(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString(isItMasterTrust = false, hasWkAdvisor = true)
        FakeUserAnswersCacheConnector.verify(SchemeNameId, "rac dac scheme")
        FakeUserAnswersCacheConnector.verify(IsMasterTrustId, false)
        FakeUserAnswersCacheConnector.verify(PSTRId, pstr)
        verify(fakeSchemeDetailsConnector, times(1)).getSchemeDetails(any(), any(), any())(any(), any())
      }

      "throw IllegalArgumentException if scheme type is missing for non rac dac scheme" in {
        val uaSchemeDetails = UserAnswers().set(GetSchemeNameId)("rac dac scheme").flatMap(_.set(PSTRId)(pstr)).asOpt.get
        val data = new FakeDataRetrievalAction(Some(UserAnswers().
          haveWorkingKnowledge(true).srn(srn).pstr(pstr).json))
        when(fakeSchemeDetailsConnector.getSchemeDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(uaSchemeDetails))
        val result = controller(data).onPageLoad()(fakeRequest)

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[IllegalArgumentException]
          e.getMessage mustEqual "Scheme Type missing for Non RacDac scheme"
        }
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }
    }

    "on a POST" must {

      "accept invitation and redirect to next page when valid data with adviser details is submitted" in {
        when(fakeInvitationCacheConnector.get(eqTo(pstr), any())(any(), any())).thenReturn(Future.successful(InvitationBuilder.invitationList))
        when(fakeInvitationCacheConnector.remove(eqTo(pstr), any())(any(), any())).thenReturn(Future.successful(()))
        when(fakeInvitationConnector.acceptInvite(eqTo(InvitationBuilder.acceptedInvitation))(any(), any())).thenReturn(Future.successful(()))

        val result = controller(data).onSubmit()(fakeRequest.withFormUrlEncodedBody("consent" -> "true"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url
        verify(fakeInvitationCacheConnector, times(1)).get(eqTo(pstr), any())(any(), any())
        verify(fakeInvitationCacheConnector, times(1)).remove(eqTo(pstr), any())(any(), any())
        verify(fakeInvitationConnector, times(1)).acceptInvite(eqTo(InvitationBuilder.acceptedInvitation))(any(), any())
      }

      "redirect to session expired page when there is no matching invitation" in {
        when(fakeInvitationCacheConnector.get(eqTo(pstr), any())(any(), any())).thenReturn(Future.successful(Nil))

        val result = controller(data).onSubmit()(fakeRequest.withFormUrlEncodedBody("consent" -> "true"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }

      "return Bad Request if invalid data is submitted" in {
        val formWithErrors = form.withError("consent", messages("messages__error__declaration__required"))
        val result = controller(data).onSubmit()(fakeRequest)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(formWithErrors)
      }

      "redirect to Session Expired page if there is no cached data" in {
        val result = controller(dontGetAnyData).onSubmit()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe sessionExpired
      }
    }
  }
}

object DeclarationControllerSpec {
  val hasAdviser = false
  val isMasterTrust = true
  val srn = "S9000000000"
  val pstr = "S12345"

  def onwardRoute: Call = Call("GET", "/foo")

  val data = new FakeDataRetrievalAction(Some(UserAnswers().
    haveWorkingKnowledge(hasAdviser).
    adviserName(InvitationBuilder.pensionAdviser.name).
    adviserEmail(InvitationBuilder.pensionAdviser.email).
    srn(srn).
    isMasterTrust(true).
    pstr(pstr).
    adviserAddress(InvitationBuilder.address).json
  ))

  val formProvider = new DeclarationFormProvider()
  val form: Form[Boolean] = formProvider()

  private def sessionExpired = controllers.routes.SessionExpiredController.onPageLoad.url
}
