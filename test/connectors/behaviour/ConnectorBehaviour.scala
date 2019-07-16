/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors.behaviour

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.{MicroserviceCacheConnector, UserAnswersCacheConnector}
import identifiers.TypedIdentifier
import org.scalatest.{AsyncWordSpec, MustMatchers, OptionValues}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.WireMockHelper
import play.api.http.Status._

import scala.reflect.ClassTag

trait ConnectorBehaviour extends AsyncWordSpec with MustMatchers with WireMockHelper with OptionValues {

  override protected def portConfigKey: String = "microservice.services.pensions-scheme.port"

  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  protected object FakeIdentifier extends TypedIdentifier[String] {
    override def toString: String = "fake-identifier"
  }

  // scalastyle:off method.length
  def cacheConnector[T <: UserAnswersCacheConnector: ClassTag](url: String => String,
                                                      lastUpdatedUrl: String => String)= {

    val connector: T = injector.instanceOf[T]

    ".fetch" must {

      "return `None` when the server returns a 404" in {

        server.stubFor(
          get(urlEqualTo(url("foo")))
            .willReturn(
              notFound
            )
        )

        connector.fetch("foo") map {
          result =>
            result mustNot be(defined)
        }
      }

      "return data when the server returns 200" in {
        server.stubFor(
          get(urlEqualTo(url("foo")))
            .willReturn(
              ok("{}")
            )
        )

        connector.fetch("foo") map {
          result =>
            result.value mustEqual Json.obj()
        }
      }


      "return a failed future on upstream error" in {

        server.stubFor(
          get(urlEqualTo(url("foo")))
            .willReturn(
              serverError
            )
        )

        recoverToExceptionIf[HttpException] {
          connector.fetch("foo")
        } map {
          _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
        }

      }
    }

    ".save" must {

      "insert when no data exists" in {

        val json = Json.obj(
          "fake-identifier" -> "foobar"
        )

        val value = Json.stringify(json)

        server.stubFor(
          get(urlEqualTo(url("foo")))
            .willReturn(
              notFound
            )
        )

        server.stubFor(
          post(urlEqualTo(url("foo")))
            .withRequestBody(equalTo(value))
            .willReturn(
              ok
            )
        )

        connector.save("foo", FakeIdentifier, "foobar") map {
          _ mustEqual json
        }
      }

      "add fields to existing data" in {

        val json = Json.obj(
          "foo" -> "bar"
        )

        val updatedJson = Json.obj(
          "foo" -> "bar",
          "fake-identifier" -> "foobar"
        )

        val value = Json.stringify(json)
        val updatedValue = Json.stringify(updatedJson)

        server.stubFor(
          get(urlEqualTo(url("foo")))
            .willReturn(
              ok(value)
            )
        )

        server.stubFor(
          post(urlEqualTo(url("foo")))
            .withRequestBody(equalTo(updatedValue))
            .willReturn(
              ok
            )
        )

        connector.save("foo", FakeIdentifier, "foobar") map {
          _ mustEqual updatedJson
        }
      }

      "update existing data" in {

        val json = Json.obj(
          "fake-identifier" -> "foo"
        )

        val updatedJson = Json.obj(
          "fake-identifier" -> "foobar"
        )

        val value = Json.stringify(json)
        val updatedValue = Json.stringify(updatedJson)

        server.stubFor(
          get(urlEqualTo(url("foo")))
            .willReturn(
              ok(value)
            )
        )

        server.stubFor(
          post(urlEqualTo(url("foo")))
            .withRequestBody(equalTo(updatedValue))
            .willReturn(
              ok
            )
        )

        connector.save("foo", FakeIdentifier, "foobar") map {
          _ mustEqual updatedJson
        }
      }

      "return a failed future on upstream error" in {

        val json = Json.obj(
          "fake-identifier" -> "foo"
        )

        val updatedJson = Json.obj(
          "fake-identifier" -> "foobar"
        )

        val value = Json.stringify(json)
        val updatedValue = Json.stringify(updatedJson)

        server.stubFor(
          get(urlEqualTo(url("foo")))
            .willReturn(
              ok(value)
            )
        )

        server.stubFor(
          post(urlEqualTo(url("foo")))
            .withRequestBody(equalTo(updatedValue))
            .willReturn(
              serverError
            )
        )


        recoverToExceptionIf[HttpException] {
          connector.save("foo", FakeIdentifier, "foobar")
        } map {
          _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
        }

      }
    }

    ".lastUpdated" must {

      "return `None` when the server returns a 404" in {

        server.stubFor(
          get(urlEqualTo(lastUpdatedUrl("foo")))
            .willReturn(
              notFound
            )
        )

        connector.lastUpdated("foo") map {
          result =>
            result mustNot be(defined)
        }
      }

      "return long value when the server returns 200" in {

        val json = Json.obj(
          "lastUpdated" -> "1528107399697"
        )

        server.stubFor(
          get(urlEqualTo(lastUpdatedUrl("foo")))
            .willReturn(
              ok(Json.stringify(json))
            )
        )

        connector.lastUpdated("foo") map {
          result =>
            result.value mustEqual json
        }
      }

      "return a failed future on upstream error" in {

        server.stubFor(
          get(urlEqualTo(lastUpdatedUrl("foo")))
            .willReturn(
              serverError
            )
        )

        recoverToExceptionIf[HttpException] {
          connector.lastUpdated("foo")
        } map {
          _.responseCode mustEqual INTERNAL_SERVER_ERROR
        }

      }
    }

    ".remove" must {
      "remove existing data" in {
        val json = Json.obj(
          FakeIdentifier.toString -> "fake value",
          "other-key" -> "meh"
        )

        val updatedJson = Json.obj(
          "other-key" -> "meh"
        )

        val value = Json.stringify(json)
        val updatedValue = Json.stringify(updatedJson)

        server.stubFor(
          get(urlEqualTo(url("foo")))
            .willReturn(
              ok(value)
            )
        )

        server.stubFor(
          post(urlEqualTo(url("foo")))
            .withRequestBody(equalTo(updatedValue))
            .willReturn(
              ok
            )
        )

        connector.remove("foo", FakeIdentifier) map {
          _ mustEqual updatedJson
        }
      }
    }

    ".removeAll" must {
      "remove all the data" in {
        server.stubFor(delete(urlEqualTo(url("foo"))).
          willReturn(ok)
        )

        connector.removeAll("foo").map {
          _ mustEqual OK
        }
      }
    }
  }
}
