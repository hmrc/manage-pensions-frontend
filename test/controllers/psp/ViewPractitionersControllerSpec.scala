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

package controllers.psp

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.{SchemeNameId, SchemeSrnId, SeqAuthorisedPractitionerId}
import models.SchemeReferenceNumber
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import viewmodels.AuthorisedPractitionerViewModel
import views.html.psp.viewPractitioners

class ViewPractitionersControllerSpec extends ControllerSpecBase {

  private val schemeName  = "Test Scheme name"
  private val schemeSrn  = "12345"
  private def returnCall: Call  = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber(schemeSrn))
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
    SchemeSrnId.toString -> schemeSrn,
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
      stubMessagesControllerComponents(),
      viewPractitionersView
    )

  private def viewAsString() = viewPractitionersView(schemeName, returnCall, practitionersViewModel)(fakeRequest, messages).toString

  "ViewPractitionersController" must {
    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad()(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

  }
}
