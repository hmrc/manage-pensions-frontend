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

package controllers.behaviours

import controllers.ControllerSpecBase
import controllers.actions._
import play.api.data.Form
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.FakeNavigator

class QuestionPageBehaviours extends ControllerSpecBase {

  val navigator = new FakeNavigator(onwardRoute)
  val requiredDateAction = new DataRequiredActionImpl

  def onwardRoute = Call("GET", "/foo")

  def onPageLoadMethod(onPageLoadAction: (DataRetrievalAction, AuthAction) => Action[AnyContent],
                       emptyData: DataRetrievalAction,
                       validDate: DataRetrievalAction,
                       emptyForm: Form[String],
                       preparedForm: Form[String],
                       validView: (Form[_]) => String): Unit = {

    "calling onPageLoad" must {

      "return OK and the correct view for a GET" in {

        val result = onPageLoadAction(emptyData, FakeAuthAction())(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe validView(emptyForm)
      }

      "populate the view correctly on a GET when the question has previously been answered" in {


        val result = onPageLoadAction(validDate, FakeAuthAction())(fakeRequest)

        contentAsString(result) mustBe validView(preparedForm)
      }

      "return 303 if user action is not authenticated" in {

        val result = onPageLoadAction(emptyData, FakeUnAuthorisedAction())(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }

  }

  def onSubmitMethod(onSubmitAction: (DataRetrievalAction, AuthAction) => Action[AnyContent],
                     validDate: DataRetrievalAction,
                     form: Form[String],
                     errorView: (Form[_]) => String,
                     postRequest: FakeRequest[AnyContentAsJson]): Unit = {

    "calling onSubmit" must {

      "redirect to the next page when valid data is submitted" in {

        val result = onSubmitAction(validDate, FakeAuthAction())(postRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)
      }

      "return a Bad Request and errors when invalid data is submitted" in {

        val result = onSubmitAction(validDate, FakeAuthAction())(fakeRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe errorView(form)
      }
    }
  }

  def requiredDataMissing(onPageLoadAction: (DataRetrievalAction, AuthAction) => Action[AnyContent],
                          onSubmitAction: (DataRetrievalAction, AuthAction) => Action[AnyContent],
                          validDate: DataRetrievalAction): Unit = {

    "when required data is not present" must {

      "onPageLoad redirect to Session Expired if no existing data is found" in {

        val result = onPageLoadAction(validDate, FakeAuthAction())(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
      }

      "onSubmit redirect to Session Expired if no existing data is found" in {

        val result = onSubmitAction(validDate, FakeAuthAction())(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
      }
    }

  }
}