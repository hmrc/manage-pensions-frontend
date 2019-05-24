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

package connectors

import base.SpecBase
import config.FeatureSwitchManagementServiceTestImpl
import connectors.cache.microservice.OldPensionsSchemeCacheConnector
import identifiers.TypedIdentifier
import org.scalatest._
import play.api.Configuration
import play.api.libs.json.{Format, JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.Toggles

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionDualCacheConnectorSpec extends AsyncWordSpec with MustMatchers with OptionValues with RecoverMethods {

  import SubscriptionDualCacheConnectorSpec._

  private object FakeIdentifier extends TypedIdentifier[String] {
    override def toString: String = "fake-identifier"
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "On toggle On" when {

    ".save is called" must {

      "save the data to old if data already exist in old and not in new collection" in {
        fakeFeatureSwitchManager.change(Toggles.isSchemeDataShiftEnabled, newValue = true)
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(true),
          fakeNewSchemeCacheConnector(false),
          fakeFeatureSwitchManager
        )
        connector.save("foo", FakeIdentifier, "") map {
          result =>
            result mustBe Json.obj("data" -> "old saved")
        }
      }

      "save the data to admin if data already exist in new and not in old collection" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(false),
          fakeNewSchemeCacheConnector(true),
          fakeFeatureSwitchManager
        )
        connector.save("foo", FakeIdentifier, "") map {
          result =>
            result mustBe Json.obj("data" -> "new saved")
        }
      }

      "save the data to new if data doesn't exist in new or old collection" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(false),
          fakeNewSchemeCacheConnector(false),
          fakeFeatureSwitchManager
        )
        connector.save("foo", FakeIdentifier, "") map {
          result =>
            result mustBe Json.obj("data" -> "new saved")
        }
      }

      "throw error when data is in new as well as old" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(true),
          fakeNewSchemeCacheConnector(true),
          fakeFeatureSwitchManager
        )
        recoverToSucceededIf[HttpException] {
          connector.save("foo", FakeIdentifier, "")
        }
      }
    }

    ".fetch  is called" must {
      "return data from old collection when the old collection has data and no data in psa collection" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(true),
          fakeNewSchemeCacheConnector(false),
          fakeFeatureSwitchManager
        )
        connector.fetch("foo") map {
          result =>
            result.value mustBe Json.obj("data" -> "old")
        }
      }

      "return data from new collection when the new collection has data and no data in old collection" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(false),
          fakeNewSchemeCacheConnector(true),
          fakeFeatureSwitchManager
        )
        connector.fetch("foo") map {
          result =>
            result.value mustBe Json.obj("data" -> "new")
        }
      }

      "throw error when data is in new as well as old" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(true),
          fakeNewSchemeCacheConnector(true),
          fakeFeatureSwitchManager
        )
        connector.fetch("foo") map {
          result =>
            result mustBe None
        }
      }
    }
    ".remove is called" must {

      "removes data from old collection when the old collection has data and no data in psa collection" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(true),
          fakeNewSchemeCacheConnector(false),
          fakeFeatureSwitchManager
        )
        connector.remove("foo", FakeIdentifier) map {
          result =>
            result mustBe Json.obj("data" -> "old removed")
        }
      }

      "removes data from new collection when the new collection has data and no data in old collection" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(false),
          fakeNewSchemeCacheConnector(true),
          fakeFeatureSwitchManager
        )
        connector.remove("foo", FakeIdentifier) map {
          result =>
            result mustBe Json.obj("data" -> "new removed")
        }
      }

      "throw error when data is in new as well as old" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(true),
          fakeNewSchemeCacheConnector(true),
          fakeFeatureSwitchManager
        )
        connector.remove("foo", FakeIdentifier) map {
          result =>
            result mustBe Json.obj()
        }
      }
    }
  }

  "On toggle Off" when {

    ".save is called" must {

      "must return data from old collection" in {
        fakeFeatureSwitchManager.change(Toggles.isSchemeDataShiftEnabled, newValue = false)
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(true),
          fakeNewSchemeCacheConnector(true),
          fakeFeatureSwitchManager
        )
        connector.save("foo", FakeIdentifier, "") map {
          result =>
            result mustBe Json.obj("data" -> "old saved")
        }
      }
    }

    ".fetch is called" must {

      "return data from old collection" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(true),
          fakeNewSchemeCacheConnector(true),
          fakeFeatureSwitchManager
        )
        connector.fetch("foo") map {
          result =>
            result.value mustBe Json.obj("data" -> "old")
        }
      }
    }
    ".remove is called" must {

      "remove data from old collection" in {
        val connector = new SubscriptionDualCacheConnector(
          frontendAppConfig,
          fakeOldSchemeCacheConnector(true),
          fakeNewSchemeCacheConnector(true),
          fakeFeatureSwitchManager
        )
        connector.remove("foo", FakeIdentifier) map {
          result =>
            result mustBe Json.obj("data" -> "old removed")
        }
      }
    }
  }

}

object SubscriptionDualCacheConnectorSpec extends SpecBase {
  val fakeWsClient = new WSClient {
    override def underlying[T]: T = ???

    override def url(url: String): WSRequest = ???

    override def close(): Unit = ???
  }

  val config = injector.instanceOf[Configuration]

  private def fetchResponse(isDataExist: Boolean, data: String) = if (isDataExist) {
    Future.successful(Some(Json.obj("data" -> data)))
  } else {
    Future.successful(None)
  }

  private def fakeOldSchemeCacheConnector(isDataExist: Boolean) = new OldPensionsSchemeCacheConnector(frontendAppConfig, fakeWsClient) {
    override def fetch(id: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[JsValue]] = fetchResponse(isDataExist, "old")

    override def save[A, I <: TypedIdentifier[A]](cacheId: String, id: I, value: A)(implicit fmt: Format[A],
                                                                                    ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] =
      Future.successful(Json.obj("data" -> "old saved"))

    override def remove[I <: TypedIdentifier[_]](cacheId: String, id: I)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] =
      Future.successful(Json.obj("data" -> "old removed"))

    override def removeAll(id: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = Future.successful(Ok("old remove all"))

    override def upsert(cacheId: String, value: JsValue)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] =
      Future.successful(Json.obj("data" -> "old upsert"))
  }

  private def fakeNewSchemeCacheConnector(isDataExist: Boolean) = new SchemeSubscriptionCacheConnector(frontendAppConfig, fakeWsClient) {
    override def fetch(id: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[JsValue]] = fetchResponse(isDataExist, "new")

    override def save[A, I <: TypedIdentifier[A]](cacheId: String, id: I, value: A)(implicit fmt: Format[A],
                                                                                    ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] =
      Future.successful(Json.obj("data" -> "new saved"))

    override def remove[I <: TypedIdentifier[_]](cacheId: String, id: I)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] =
      Future.successful(Json.obj("data" -> "new removed"))

  }

  val fakeFeatureSwitchManager = new FeatureSwitchManagementServiceTestImpl(config, environment)
}