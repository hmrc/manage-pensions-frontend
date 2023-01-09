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

import controllers.ControllerSpecBase
import controllers.actions._
import controllers.invitations.psa.routes.PsaNameController
import controllers.psa.routes._
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{NormalMode, SchemeReferenceNumber}
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.Helpers._
import views.html.invitations.psa.whatYouWillNeed

class WhatYouWillNeedControllerSpec extends ControllerSpecBase {

  val schemeName  = "Test Scheme name"
  val schemeSrn  = "12345"
  val returnCall: Call  = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(schemeSrn))

  val validData = new FakeDataRetrievalAction(Some(Json.obj(
    SchemeSrnId.toString -> schemeSrn,
    SchemeNameId.toString -> schemeName
  )))

  private val whatYouWillNeedView = injector.instanceOf[whatYouWillNeed]

  def controller(dataRetrievalAction: DataRetrievalAction = validData): WhatYouWillNeedController =
    new WhatYouWillNeedController(
      messagesApi,
      FakeAuthAction,
      dataRetrievalAction,
      new DataRequiredActionImpl,
      controllerComponents,
      whatYouWillNeedView
    )

  private def viewAsString() = whatYouWillNeedView(schemeName, returnCall)(fakeRequest, messages).toString

  "WhatYouWillNeedController" must {
    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad()(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "redirect to psa name controller for a POST" in {
      val result = controller().onSubmit()(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe
        Some(PsaNameController.onPageLoad(NormalMode).url)
    }
  }
}
