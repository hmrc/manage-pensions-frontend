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

package controllers.psa.remove

import base.SpecBase
import connectors.FakeUserAnswersCacheConnector
import connectors.admin.MinimalConnector
import controllers.actions._
import controllers.behaviours.ControllerWithNormalPageBehaviours
import identifiers.{SchemeNameId, SeqAuthorisedPractitionerId}
import models.MinimalPSAPSP
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{Action, AnyContent}
import views.html.remove.psa.confirmPsaRemovedPsp

import scala.concurrent.Future

class ConfirmPsaRemovedPspControllerSpec
  extends ControllerWithNormalPageBehaviours
    with MockitoSugar
    with BeforeAndAfterEach {

  import ConfirmPsaRemovedPspControllerSpec._

  private val view = injector.instanceOf[confirmPsaRemovedPsp]
  private val mockMinimalPsaConnector: MinimalConnector = mock[MinimalConnector]

  override def beforeEach(): Unit = {
    reset(mockMinimalPsaConnector)
    when(mockMinimalPsaConnector.getMinimalPsaDetails(any())(any(), any())).thenReturn(Future.successful(minPsa))
  }

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction = sessionData,
                       fakeAuth: AuthAction = FakeAuthAction): Action[AnyContent] =
    new ConfirmPsaRemovedPspController(
      messagesApi = messagesApi,
      authenticate = fakeAuth,
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      userAnswersCacheConnector = FakeUserAnswersCacheConnector,
      controllerComponents = controllerComponents,
      minimalPsaConnector = mockMinimalPsaConnector,
      view = view
    ).onPageLoad(0)

  def viewAsString(base: SpecBase)(): String =
    view(
      pspName = pspName,
      schemeName = schemeName,
      psaEmailAddress = psaEmail
    )(base.fakeRequest, base.messages).toString()

  "ConfirmPsaRemovedPspController" should {

    behave like controllerWithOnPageLoadMethod(
      onPageLoadAction = onPageLoadAction,
      emptyData = getEmptyData,
      validData = Some(sessionData),
      validView = viewAsString(this)
    )
  }
}

object ConfirmPsaRemovedPspControllerSpec {

  private val pspName = "PSP Limited Company 1"
  private val schemeName = "test-scheme-name"
  private val psaEmail = "a@b.com"
  private val minPsa = MinimalPSAPSP(
    email = psaEmail,
    isPsaSuspended = false,
    organisationName = Some(schemeName),
    individualDetails = None,
    rlsFlag = false,
    deceasedFlag = false
  )

  private val practitioners = JsArray(
    Seq(
      Json.obj(
        "authorisingPSAID" -> "A0000000",
        "authorisingPSA" -> Json.obj(
          "firstName" -> "Nigel",
          "lastName" -> "Smith",
          "middleName" -> "Robert"
        ),
        "relationshipStartDate" -> "2020-04-01",
        "id" -> "A2200005",
        "organisationOrPartnershipName" -> "PSP Limited Company 1"
      ),
      Json.obj(
        "authorisingPSAID" -> "A2100007",
        "authorisingPSA" -> Json.obj(
          "organisationOrPartnershipName" -> "Acme Ltd"
        ),
        "relationshipStartDate" -> "2020-04-01",
        "id" -> "A2200007",
        "individual" -> Json.obj(
          "firstName" -> "PSP Individual",
          "lastName" -> "Second"
        )
      )
    )
  )

  private val data = Json.obj(
    SchemeNameId.toString -> schemeName,
    SeqAuthorisedPractitionerId.toString -> practitioners,
  )

  private val sessionData: FakeDataRetrievalAction =
    new FakeDataRetrievalAction(Some(data))
}


