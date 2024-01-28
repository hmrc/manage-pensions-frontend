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

package utils

import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status._
import uk.gov.hmrc.http._

// scalastyle:off magic.number

class HttpResponseHelperSpec extends AnyFlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  import HttpResponseHelperSpec._

  "handleErrorResponse" should "transform Bad Request into BadRequestException" in {
    val response = responseFor(BAD_REQUEST)
    a[BadRequestException] should be thrownBy fixture()(response)
  }

  it should "transform Not Found into NotFoundException" in {
    val response = responseFor(NOT_FOUND)
    a[NotFoundException] should be thrownBy fixture()(response)
  }

  it should "transform any other 4xx into Upstream4xxResponse" in {
    val userErrors = for (n <- Gen.choose(400, 499) suchThat (n => n != 400 && n != 404)) yield n

    forAll(userErrors) {
      userError =>
        val ex = the[UpstreamErrorResponse] thrownBy fixture()(responseFor(userError))
        ex.reportAs shouldBe userError
        ex.statusCode shouldBe userError
    }
  }

  it should "transform any 5xx into Upstream5xxResponse" in {
    val serverErrors = for (n <- Gen.choose(500, 599)) yield n

    forAll(serverErrors) {
      serverError =>
        val ex = the[UpstreamErrorResponse] thrownBy fixture()(responseFor(serverError))
        ex.reportAs shouldBe BAD_GATEWAY
        ex.statusCode shouldBe serverError
    }
  }

  it should "transform any other status into an UnrecognisedHttpResponseException" in {
    val statuses = for (n <- Gen.choose(0, 1000) suchThat (n => n < 400 || n >= 600)) yield n

    forAll(statuses) {
      status =>
        an[UnrecognisedHttpResponseException] should be thrownBy fixture()(responseFor(status))
    }
  }

}

object HttpResponseHelperSpec {

  def fixture(): HttpResponse => Nothing = {
    new HttpResponseHelper {}.handleErrorResponse("test-mnethod", "test-url")
  }

  def responseFor(status: Int): HttpResponse = HttpResponse(status, s"Message for $status")


}
