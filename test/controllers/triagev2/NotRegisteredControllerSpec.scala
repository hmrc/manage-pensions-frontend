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

package controllers.triagev2

import controllers.ControllerSpecBase
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, defaultAwaitTimeout, route, status, writeableOf_AnyContentAsEmpty}
import utils.annotations.TriageV2
import utils.{FakeNavigator, Navigator}

class NotRegisteredControllerSpec extends ControllerSpecBase with ScalaFutures with MockitoSugar {

  private val onwardRoute = Call("GET", "/dummy")
  private val application = applicationBuilder(Seq[GuiceableModule](bind[Navigator].
    qualifiedWith(classOf[TriageV2]).toInstance(new FakeNavigator(onwardRoute)))).build()

  "NotRegisteredController" must {

    "return OK with the view when calling on page load" in {

      val request = FakeRequest(GET, routes.NotRegisteredController.onPageLoad.url)
      val result = route(application, request).value

      status(result) mustBe OK
    }
  }

}
