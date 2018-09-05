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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, AsyncFlatSpec}
import play.api.http.Status._
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.http.{Upstream5xxResponse, HeaderCarrier, HttpResponse}
import utils.WireMockHelper

class AssociationConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with Checkers {

  override protected def portConfigKey: String = "microservice.services.pension-administrator.port"

  import AssociationConnectorSpec._

  "calling getSubscriptionDetails" should "return 200" in {

    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(OK).withBody(successResponse)
        )
    )

    val connector = injector.instanceOf[AssociationConnector]

    connector.getSubscriptionDetails(psaId).map {
      result =>
        Json.toJson(result) shouldBe Json.parse(successResponse)
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }

  }

  it should "throw exception if failed to parse the json" in {

    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(OK).withBody(invalidResponse)
        )
    )

    val connector = injector.instanceOf[AssociationConnector]

    recoverToExceptionIf[JsResultException] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }

  }

  it should "throw badrequest if INVALID_PSAID" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(400).withBody("INVALID_PSAID")
        )
    )

    val connector = injector.instanceOf[AssociationConnector]


    recoverToExceptionIf[PsaIdInvalidException] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }

  it should "throw badrequest if INVALID_CORRELATIONID" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(400).withBody("INVALID_CORRELATIONID")
        )
    )

    val connector = injector.instanceOf[AssociationConnector]


    recoverToExceptionIf[CorrelationIdInvalidException] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }
  it should "throw Not Found" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          notFound()
        )
    )

    val connector = injector.instanceOf[AssociationConnector]


    recoverToExceptionIf[PsaIdNotFoundException] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }

  it should "throw Upstream5xxResponse for internal server error" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          serverError()
        )
    )

    val connector = injector.instanceOf[AssociationConnector]


    recoverToExceptionIf[Upstream5xxResponse] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }

  it should "throw Generic exception for all others" in {
    server.stubFor(
      get(urlEqualTo(subscriptionDetailsUrl)).withHeader("psaId", equalTo(psaId))
        .willReturn(
          serverError()
        )
    )

    val connector = injector.instanceOf[AssociationConnector]


    recoverToExceptionIf[Exception] {
      connector.getSubscriptionDetails(psaId)
    } map {
      _ =>
        server.findAll(getRequestedFor(urlEqualTo(subscriptionDetailsUrl))
          .withHeader("psaId", equalTo(psaId))).size() shouldBe 1
    }
  }

}

object AssociationConnectorSpec {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  val psaId = "A1234567"
  val subscriptionDetailsUrl = s"/pension-administrator/psa-subscription-details"
  val psaIdJson = Json.stringify(
    Json.obj(
      "psaId" -> s"$psaId"
    )
  )
  val invalidResponse = """{"invalid" : "response"}"""

  val successResponse = """{
                         |	"psaSubscriptionDetails": {
                         |		"isPSASuspension": true,
                         |		"customerIdentificationDetails": {
                         |			"legalStatus": "Individual",
                         |			"idType": "NINO",
                         |			"idNumber": "AA999999A",
                         |			"noIdentifier": true
                         |		},
                         |		"individualDetails": {
                         |			"title": "Mr",
                         |			"firstName": "abcdefghijkl",
                         |			"middleName": "abcdefghijkl",
                         |			"lastName": "abcdefjkl",
                         |			"dateOfBirth": "1947-03-29"
                         |		},
                         |		"correspondenceAddressDetails": {
                         |			"nonUKAddress": false,
                         |			"line1": "Telford1",
                         |			"line2": "Telford2",
                         |			"line3": "Telford3",
                         |			"line4": "Telford3",
                         |			"postalCode": "TF3 4ER",
                         |			"countryCode": "GB"
                         |		},
                         |		"correspondenceContactDetails": {
                         |			"telephone": " ",
                         |			"mobileNumber": " ",
                         |			"fax": " ",
                         |			"email": "aaa@aa.com"
                         |		},
                         |		"previousAddressDetails": {
                         |			"isPreviousAddressLast12Month": true,
                         |			"previousAddress": {
                         |				"nonUKAddress": false,
                         |				"line1": "London1",
                         |				"line2": "London2",
                         |				"line3": "London3",
                         |				"line4": "London4",
                         |				"postalCode": "LN12 4DC",
                         |				"countryCode": "GB"
                         |			}
                         |		},
                         |		"numberOfDirectorsOrPartnersDetails": {
                         |			"isMorethanTenDirectors": true,
                         |			"isMorethanTenPartners": true
                         |		},
                         |		"directorOrPartnerDetails": [{
                         |				"sequenceId": "123",
                         |				"entityType": "Director",
                         |				"title": "Mr",
                         |				"firstName": "abcdef",
                         |				"middleName": "dfgdsfff",
                         |				"lastName": "dfgfdgfdg",
                         |				"dateOfBirth": "1950-03-29",
                         |				"nino": "AA999999A",
                         |				"noNinoReason": "dffdffdfsf",
                         |				"utr": "1234567892",
                         |				"noUtrReason": "sfdsfsdf",
                         |				"correspondenceCommonDetails": {
                         |					"addressDetails": {
                         |						"nonUKAddress": true,
                         |						"line1": "addressline1",
                         |						"line2": "addressline2",
                         |						"line3": "addressline3",
                         |						"line4": "addressline4",
                         |						"postalCode": "B5 9EX",
                         |						"countryCode": "GB"
                         |					},
                         |					"contactDetails": {
                         |						"telephone": "0044-09876542312",
                         |						"mobileNumber": "0044-09876542312",
                         |						"fax": "0044-09876542312",
                         |						"email": "abc@hmrc.gsi.gov.uk"
                         |					}
                         |				},
                         |				"previousAddressDetails": {
                         |					"isPreviousAddressLast12Month": true,
                         |					"previousAddress": {
                         |						"nonUKAddress": true,
                         |						"line1": "line1",
                         |						"line2": "line2",
                         |						"line3": "line3",
                         |						"line4": "line4",
                         |						"postalCode": "567253",
                         |						"countryCode": "AD"
                         |					}
                         |				}
                         |			}, {
                         |				"sequenceId": "124",
                         |				"entityType": "Director",
                         |				"title": "Mr",
                         |				"firstName": "sdfdff",
                         |				"middleName": "sdfdsfsdf",
                         |				"lastName": "dfdsfsf",
                         |				"dateOfBirth": "1950-07-29",
                         |				"nino": "AA999999A",
                         |				"noNinoReason": "fsdfsf",
                         |				"utr": "7897700000",
                         |				"noUtrReason": "dfgfdg",
                         |				"correspondenceCommonDetails": {
                         |					"addressDetails": {
                         |						"nonUKAddress": true,
                         |						"line1": "fgfdgdfgfd",
                         |						"line2": "dfgfdgdfg",
                         |						"line3": "fdrtetegfdgdg",
                         |						"line4": "dfgfdgdfg",
                         |						"postalCode": "56546",
                         |						"countryCode": "AD"
                         |					},
                         |					"contactDetails": {
                         |						"telephone": "0044-09876542334",
                         |						"mobileNumber": "0044-09876542312",
                         |						"fax": "0044-09876542312",
                         |						"email": "aaa@gmail.com"
                         |					}
                         |				},
                         |				"previousAddressDetails": {
                         |					"isPreviousAddressLast12Month": true,
                         |					"previousAddress": {
                         |						"nonUKAddress": true,
                         |						"line1": "werrertqe",
                         |						"line2": "ereretfdg",
                         |						"line3": "asafafg",
                         |						"line4": "fgdgdasdf",
                         |						"postalCode": "23424",
                         |						"countryCode": "AD"
                         |					}
                         |				}
                         |			}
                         |		],
                         |		"declarationDetails": {
                         |			"box1": true,
                         |			"box2": true,
                         |			"box3": true,
                         |			"box4": true,
                         |			"box5": true,
                         |			"box6": true,
                         |			"box7": true,
                         |			"pensionAdvisorDetails": {
                         |				"name": "sgfdgssd",
                         |				"addressDetails": {
                         |					"nonUKAddress": true,
                         |					"line1": "addline1",
                         |					"line2": "addline2",
                         |					"line3": "addline3",
                         |					"line4": "addline4 ",
                         |					"postalCode": "56765",
                         |					"countryCode": "AD"
                         |				},
                         |				"contactDetails": {
                         |					"telephone": "0044-0987654232",
                         |					"mobileNumber": "0044-09876542335",
                         |					"fax": "0044-098765423353",
                         |					"email": "aaa@yahoo.com"
                         |				}
                         |			}
                         |		}
                         |	}
                         |}""".stripMargin
}