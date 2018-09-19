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

package utils

import connectors.{DataCacheConnector, FakeDataCacheConnector}
import identifiers.{LastPageId, TypedIdentifier}
import models.requests.IdentifiedRequest
import models.{CheckMode, LastPage, NormalMode}
import org.scalatest.{MustMatchers, WordSpec}
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

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

    "in either mode" must {
      "save the last page when configured to do so" in {
        val fixture = testFixture()
        val result = fixture.navigator.nextPage(testSaveId, NormalMode, UserAnswers())
        result mustBe testSaveCall
        fixture.dataCacheConnector.verify(LastPageId, LastPage(testSaveCall.method, testSaveCall.url))
      }

      "not save the last page when configured not to" in {
        val fixture = testFixture()
        val result = fixture.navigator.nextPage(testNotSaveId, NormalMode, UserAnswers())
        result mustBe testNotSaveCall
        fixture.dataCacheConnector.verifyNot(LastPageId)
      }
    }

  }

}

object NavigatorSpec {

  val testNotExistCall: Call = Call("GET", "http://www.test.com/not-exist")
  val testExistNormalModeCall: Call = Call("GET", "http://www.test.com/exist/normal-mode")
  val testExistCheckModeCall: Call = Call("GET", "http://www.test.com/exist/check-mode")
  val testSaveCall: Call = Call("GET", "http://www.test.com/save")
  val testNotSaveCall: Call = Call("GET", "http://www.test.com/not-save")

  val testExistId: TypedIdentifier[Nothing] = new TypedIdentifier[Nothing] {}
  val testNotExistId: TypedIdentifier[Nothing] = new TypedIdentifier[Nothing] {}
  val testSaveId: TypedIdentifier[Nothing] = new TypedIdentifier[Nothing] {}
  val testNotSaveId: TypedIdentifier[Nothing] = new TypedIdentifier[Nothing] {}

  class TestNavigator(val dataCacheConnector: DataCacheConnector) extends Navigator {

    override protected def routeMap(from: NavigateFrom): Option[NavigateTo] =
      from.id match {
        case `testExistId` => NavigateTo.dontSave(testExistNormalModeCall)
        case `testSaveId` => NavigateTo.save(testSaveCall)
        case `testNotSaveId` => NavigateTo.dontSave(testNotSaveCall)
        case _ => None
      }

    override protected def editRouteMap(from: NavigateFrom): Option[NavigateTo] =
      from.id match {
        case `testExistId` => NavigateTo.dontSave(testExistCheckModeCall)
        case _ => None
      }

  }

  trait TestFixture {
    def dataCacheConnector: FakeDataCacheConnector

    def navigator: TestNavigator
  }

  def testFixture(): TestFixture = new TestFixture {
    override val dataCacheConnector: FakeDataCacheConnector = new FakeDataCacheConnector {}
    override val navigator: TestNavigator = new TestNavigator(dataCacheConnector)
  }

  implicit val ex: IdentifiedRequest = new IdentifiedRequest() {
    val externalId: String = "test-external-id"
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()

}
