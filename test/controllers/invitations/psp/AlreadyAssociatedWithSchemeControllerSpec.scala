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

package controllers.invitations.psp

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.SchemeNameId
import identifiers.invitations.psp.PspNameId
import play.api.test.Helpers._
import utils.UserAnswers
import views.html.invitations.psp.alreadyAssociatedWithScheme


class AlreadyAssociatedWithSchemeControllerSpec extends ControllerSpecBase {

  private val schemeName = "Test Scheme"
  private val userAnswer = UserAnswers()
    .set(PspNameId)("xyz").asOpt.value
    .set(SchemeNameId)(schemeName).asOpt.value
  private val minimalData = new FakeDataRetrievalAction(Some(userAnswer.json))

  private val view = injector.instanceOf[alreadyAssociatedWithScheme]

  def controller(dataRetrievalAction: DataRetrievalAction = minimalData) = new AlreadyAssociatedWithSchemeController(
    messagesApi,
    FakeAuthAction,
    dataRetrievalAction,
    new DataRequiredActionImpl,
    controllerComponents,
    view
  )

  private def viewAsString(): String = view("xyz", schemeName)(fakeRequest, messages).toString

  "PspClientReferenceController" when {
    "on a GET" must {

      "return OK and the correct view" in {
        val result = controller().onPageLoad()(fakeRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }

      "redirect to the session expired page if there is no psp name" in {
        val result = controller(getEmptyData).onPageLoad()(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }

      "redirect to the session expired page if there is no existing data" in {
        val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)
        redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad.url
      }
    }
  }
}




