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

package controllers.invitations

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.SchemeDetailId
import models.MinimalSchemeDetail
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import views.html.invitations.invitationAccepted

class InvitationAcceptedControllerSpec extends ControllerSpecBase {

  val testSchemeName = "test-scheme-name"

  val validData: JsObject = Json.obj(
    SchemeDetailId.toString -> Json.toJson(MinimalSchemeDetail("srn", Some("pstr"), testSchemeName))
  )

  def controller(dataRetrievalAction: DataRetrievalAction =
    new FakeDataRetrievalAction(Some(validData))): InvitationAcceptedController =
      new InvitationAcceptedController(
        frontendAppConfig,
        messagesApi,
        FakeAuthAction(),
        dataRetrievalAction,
        new DataRequiredActionImpl
      )

  def viewAsString(): String = invitationAccepted(frontendAppConfig, Some(testSchemeName))(fakeRequest, messages).toString

  "InvitationAccepted Controller" must {
    "return OK with correct content on GET" in {
      val result = controller().onPageLoad(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }
  }
}
