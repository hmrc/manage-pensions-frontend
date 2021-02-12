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

package utils

import identifiers.{Identifier, TypedIdentifier}
import models.requests.IdentifiedRequest
import models.{CheckMode, NormalMode}
import org.scalatest.{MustMatchers, WordSpec}
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier

class NavigatorSpec extends WordSpec with MustMatchers {

  import NavigatorSpec._

  "Navigator" when {

    "in Normal mode" must {
      "go to the specified page for an identifier that does exist in the route map" in {
        val fixture = testFixture()
        val result = fixture.navigator.nextPage(testExistId, NormalMode, UserAnswers())
        result mustBe testExistNormalModeCall
      }

      "go to Index from an identifier that doesn't exist in the route map" in {
        val fixture = testFixture()
        val result = fixture.navigator.nextPage(testNotExistId, NormalMode, UserAnswers())
        result mustBe controllers.routes.IndexController.onPageLoad()
      }
    }

    "in Check mode" must {
      "go to the specified page for an identifier that does exist in the route map" in {
        val fixture = testFixture()
        val result = fixture.navigator.nextPage(testExistId, CheckMode, UserAnswers())
        result mustBe testExistCheckModeCall
      }

      "go to Index from an identifier that doesn't exist in the edit route map" in {
        val fixture = testFixture()
        val result = fixture.navigator.nextPage(testNotExistId, CheckMode, UserAnswers())
        result mustBe controllers.routes.IndexController.onPageLoad()
      }
    }
  }
}

object NavigatorSpec {

  val testNotExistCall: Call = Call("GET", "http://www.test.com/not-exist")
  val testExistNormalModeCall: Call = Call("GET", "http://www.test.com/exist/normal-mode")
  val testExistCheckModeCall: Call = Call("GET", "http://www.test.com/exist/check-mode")

  val testExistId: TypedIdentifier[Nothing] = new TypedIdentifier[Nothing] {}
  val testNotExistId: TypedIdentifier[Nothing] = new TypedIdentifier[Nothing] {}

  class TestNavigator extends Navigator {

    override protected def routeMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
      case `testExistId` => testExistNormalModeCall
    }

    override protected def editRouteMap(ua: UserAnswers): PartialFunction[Identifier, Call] = {
      case `testExistId` => testExistCheckModeCall
    }

  }

  trait TestFixture {
    def navigator: TestNavigator
  }

  def testFixture(): TestFixture = new TestFixture {
    override val navigator: TestNavigator = new TestNavigator
  }

  implicit val ex: IdentifiedRequest = new IdentifiedRequest() {
    val externalId: String = "test-external-id"
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()

}
