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

package controllers.invitations

import config.FrontendAppConfig
import connectors.FakeDataCacheConnector
import controllers.actions.{AuthAction, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.invitations.PensionAdviserAddressListFormProvider
import identifiers.TypedIdentifier
import identifiers.invitations.{AdviserAddressId, AdviserPostCodeLookupId}
import models.{Address, TolerantAddress}
import org.scalatest.{Matchers, WordSpec}
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.{FakeNavigator, Navigator}
import views.html.invitations.pension_adviser_address_list

class PensionAdviserAddressListControllerSpec extends WordSpec with Matchers {

  import PensionAdviserAddressListControllerSpec._

  def dataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj(
    AdviserPostCodeLookupId.toString -> addresses
  )))

  "get" must {

    "return Ok and the correct view when no addresses" in {

      running(_.overrides(
        bind[Navigator].toInstance(FakeNavigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onPageLoad()(FakeRequest())

        status(result) shouldBe OK
        contentAsString(result) shouldBe viewAsString(app, None)
      }

    }

    "return Ok and the correct view when addresses are supplied" in {

      running(_.overrides(
        bind[Navigator].toInstance(FakeNavigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onPageLoad()(FakeRequest())

        status(result) shouldBe OK
        contentAsString(result) shouldBe viewAsString(app, None)
      }

    }

  }

  "post" must {

    "return See Other on submission of valid data" in {

      running(_.overrides(
        bind[Navigator].toInstance(FakeNavigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onSubmit()(FakeRequest().withFormUrlEncodedBody("value"->"1"))

        status(result) shouldBe SEE_OTHER
      }

    }

    "redirect to the page specified by the navigator following submission of valid data" in {

      running(_.overrides(
        bind[Navigator].toInstance(FakeNavigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onSubmit()(FakeRequest().withFormUrlEncodedBody("value" -> "1"))

        redirectLocation(result) shouldBe Some(FakeNavigator.desiredRoute)
      }

    }

    "save the user answer on submission of valid data" in {

      running(_.overrides(
        bind[Navigator].toInstance(FakeNavigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onSubmit()(FakeRequest().withFormUrlEncodedBody("value" -> "1"))

        status(result) shouldBe SEE_OTHER
        FakeDataCacheConnector.verify(AdviserAddressId.toString, addresses.head)
      }

    }

    "delete any existing address on submission of valid data" in {

      running(_.overrides(
        bind[Navigator].toInstance(FakeNavigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onSubmit()(FakeRequest().withFormUrlEncodedBody("value"->"1"))

        status(result) shouldBe SEE_OTHER
        FakeDataCacheConnector.verifyNot(fakeAddressId)
      }

    }

    "return Bad Request and the correct view on submission of invalid data" in {

      running(_.overrides(
        bind[Navigator].toInstance(FakeNavigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onSubmit()(FakeRequest())

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldBe viewAsString(app, Some(-1))
      }

    }

  }

}

object PensionAdviserAddressListControllerSpec {

  val onwardRoute: Call = controllers.routes.IndexController.onPageLoad()

  private val addresses = Seq(
    TolerantAddress(
      Some("Address 1 Line 1"),
      Some("Address 1 Line 2"),
      Some("Address 1 Line 3"),
      Some("Address 1 Line 4"),
      Some("A1 1PC"),
      Some("GB")
    ),
    TolerantAddress(
      Some("Address 2 Line 1"),
      Some("Address 2 Line 2"),
      Some("Address 2 Line 3"),
      Some("Address 2 Line 4"),
      Some("123"),
      Some("FR")
    )
  )

  def viewAsString(app: Application, value: Option[Int]): String = {

    val appConfig = app.injector.instanceOf[FrontendAppConfig]
    val request = FakeRequest()
    val messages = app.injector.instanceOf[MessagesApi].preferred(request)

    val form = value match {
      case Some(i) => new PensionAdviserAddressListFormProvider()(addresses).bind(Map("value" -> i.toString))
      case None => new PensionAdviserAddressListFormProvider()(addresses)
    }

    pension_adviser_address_list(appConfig, form, addresses)(request, messages).toString()

  }

}
