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

package controllers.psp

import connectors.FakeUserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.{SchemeNameId, SchemeSrnId}
import models.SchemeReferenceNumber
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeNavigator
import viewmodels.AuthorisedPractitioner
import views.html.psp.viewPractitioners

class ViewPractitionersControllerSpec extends ControllerSpecBase {

  val schemeName  = "Test Scheme name"
  val schemeSrn  = "12345"
  val returnCall: Call  = controllers.routes.SchemeDetailsController.onPageLoad(SchemeReferenceNumber(schemeSrn))
  val practitioners = Seq(AuthorisedPractitioner("Joe Bloggs", "Ann Bloggs", "02-01-2020"))

  val validData = new FakeDataRetrievalAction(Some(Json.obj(
    SchemeSrnId.toString -> schemeSrn,
    SchemeNameId.toString -> schemeName
  )))

  private val viewPractitionersView = injector.instanceOf[viewPractitioners]

  def controller(dataRetrievalAction: DataRetrievalAction = validData): ViewPractitionersController =
    new ViewPractitionersController(
      frontendAppConfig,
      messagesApi,
      FakeNavigator,
      FakeAuthAction(),
      dataRetrievalAction,
      new DataRequiredActionImpl,
      FakeUserAnswersCacheConnector,
      stubMessagesControllerComponents(),
      viewPractitionersView
    )

  private def viewAsString() = viewPractitionersView(schemeName, returnCall, practitioners)(fakeRequest, messages).toString

  "ViewPractitionersController" must {
    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad()(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

  }
}