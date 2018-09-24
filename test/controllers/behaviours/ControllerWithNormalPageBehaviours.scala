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
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.FakeNavigator

class ControllerWithNormalPageBehaviours extends ControllerSpecBase {

  val navigator = new FakeNavigator(onwardRoute)
  val requiredDateAction = new DataRequiredActionImpl

  def onwardRoute = Call("GET", "/foo")

  def controllerWithOnPageLoadMethod[T](onPageLoadAction: (DataRetrievalAction, AuthAction) => Action[AnyContent],
                                        emptyData: DataRetrievalAction,
                                        validDate: Option[DataRetrievalAction],
                                        validView: () => String): Unit = {

    "calling onPageLoad" must {

      if (validDate.isDefined) {
        "populate the view correctly on a GET when the required data to render page is present" in {

          val result = onPageLoadAction(validDate.getOrElse(getEmptyData), FakeAuthAction())(fakeRequest)

          contentAsString(result) mustBe validView()
        }

        "onPageLoad redirect to Session Expired if no existing data is found" in {

          val result = onPageLoadAction(emptyData, FakeAuthAction())(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
        }
      } else {

        "return OK and the correct view for a GET" in {

          val result = onPageLoadAction(emptyData, FakeAuthAction())(fakeRequest)

          status(result) mustBe OK
          contentAsString(result) mustBe validView()
        }
      }

      "return 303 if user action is not authenticated" in {

        val result = onPageLoadAction(emptyData, FakeUnAuthorisedAction())(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  def controllerWithOnSubmitMethod[T](onSubmitAction: (DataRetrievalAction, AuthAction) => Action[AnyContent],
                                      emptyData: DataRetrievalAction,
                                      validDate: Option[DataRetrievalAction]): Unit = {

    "calling onSubmit" must {

      "redirect to the next page when valid data is present" in {

        val result = onSubmitAction(emptyData, FakeAuthAction())(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardRoute.url)
      }

      if (validDate.isDefined) {

        "return a Bad Request and errors when invalid data is submitted" in {

          val result = onSubmitAction(validDate.getOrElse(emptyData), FakeAuthAction())(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
        }
      }
    }
  }


}