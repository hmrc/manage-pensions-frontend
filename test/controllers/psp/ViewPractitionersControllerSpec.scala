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

package controllers.psp

import controllers.ControllerSpecBase
import controllers.actions._
import controllers.psa.routes._
import identifiers.{SchemeNameId, SchemeSrnId, SeqAuthorisedPractitionerId}
import models.SchemeReferenceNumber
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import viewmodels.AuthorisedPractitionerViewModel
import views.html.psp.viewPractitioners

import scala.concurrent.Future

class ViewPractitionersControllerSpec extends ControllerSpecBase with MockitoSugar {

  private val schemeName  = "Test Scheme name"
  private def returnCall: Call  = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))
  private val practitionersViewModel = Seq(
    AuthorisedPractitionerViewModel("PSP Limited Company 1", "Nigel Robert Smith", "1 April 2021", false),
    AuthorisedPractitionerViewModel("PSP Individual Second", "Acme Ltd", "1 April 2021", false)
  )

  private val practitioners = JsArray(
    Seq(
      Json.obj(
          "authorisingPSAID" -> "A2100005",
          "authorisingPSA" -> Json.obj(
            "firstName" -> "Nigel",
            "lastName" -> "Smith",
            "middleName" -> "Robert"
          ),
          "relationshipStartDate" -> "2021-04-01",
          "id" -> "A2200005",
          "organisationOrPartnershipName" -> "PSP Limited Company 1"
      ),
      Json.obj(
        "authorisingPSAID" -> "A2100007",
        "authorisingPSA" -> Json.obj(
          "organisationOrPartnershipName" -> "Acme Ltd"
        ),
        "relationshipStartDate" -> "2021-04-01",
        "id" -> "A2200007",
        "individual" -> Json.obj(
          "firstName" -> "PSP Individual",
          "lastName" -> "Second"
        )
      )
    )
  )

  private val validData = new FakeDataRetrievalAction(Some(Json.obj(
    SchemeSrnId.toString -> srn,
    SchemeNameId.toString -> schemeName,
    SeqAuthorisedPractitionerId.toString -> practitioners
  )))

  private val viewPractitionersView = injector.instanceOf[viewPractitioners]

  private def controller(dataRetrievalAction: DataRetrievalAction = validData): ViewPractitionersController =
    new ViewPractitionersController(
      messagesApi,
      FakeAuthAction,
      dataRetrievalAction,
      new DataRequiredActionImpl,
      controllerComponents,
      viewPractitionersView,
      fakePsaSchemeAuthAction
    )

  private def viewAsString() = viewPractitionersView(schemeName, returnCall, practitionersViewModel, true, srn.id)(fakeRequest, messages).toString

  "ViewPractitionersController" must {
    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

  }
}
