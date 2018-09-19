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

package controllers

import controllers.actions._
import identifiers.{ListOfSchemesId, PsaNameId}
import models.{Index, SchemeDetail}
import play.api.libs.json.Json
import play.api.test.Helpers._
import views.html.incorrectPsaDetails

class IncorrectPsaDetailsControllerSpec extends ControllerSpecBase {

  "IncorrectPsaDetails Controller" must {

    val invitee = "PSA"
    val schemeName = "Scheme"
    val scheme = SchemeDetail(schemeName, "schemeRef", "OK", None, None,None)
    val FakeDataRetrieval = new FakeDataRetrievalAction(Some(Json.obj(
      PsaNameId.toString -> invitee,
      ListOfSchemesId.toString -> List(scheme)
    )))
    val DataRequiredAction = new DataRequiredActionImpl()

    val controller = new IncorrectPsaDetailsController(
      frontendAppConfig,
      messagesApi,
      FakeAuthAction(),
      FakeDataRetrieval,
      DataRequiredAction
    )

    "return 200 for a GET" in {
      val result = controller.onPageLoad(Index(1))(fakeRequest)
      status(result) mustBe OK
    }

    "return the correct view for a GET" in {
      val result = controller.onPageLoad(Index(1))(fakeRequest)
      contentAsString(result) mustBe incorrectPsaDetails(frontendAppConfig, invitee)(fakeRequest, messages).toString
    }
  }
}
