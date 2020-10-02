/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.FakeUserAnswersCacheConnector
import connectors.UserAnswersCacheConnector
import controllers.actions.AuthAction
import controllers.actions.DataRetrievalAction
import controllers.actions.FakeAuthAction
import controllers.actions.FakeDataRetrievalAction
import forms.invitations.PensionAdviserAddressListFormProvider
import identifiers.invitations.AdviserAddressId
import identifiers.invitations.AdviserAddressListId
import identifiers.invitations.AdviserAddressPostCodeLookupId
import models.NormalMode
import models.TolerantAddress
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Matchers
import org.scalatest.WordSpec
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.annotations.AcceptInvitation
import utils.FakeNavigator
import utils.Navigator
import views.html.invitations.pension_adviser_address_list

class PensionAdviserAddressListControllerSpec extends WordSpec with Matchers with BeforeAndAfterEach {

  import PensionAdviserAddressListControllerSpec._

  def dataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj(
    AdviserAddressPostCodeLookupId.toString -> addresses
  )))

  override def beforeEach(): Unit = {
    FakeUserAnswersCacheConnector.reset()
  }

  "get" must {

    "return Ok and the correct view when no addresses" in {

      running(_.overrides(
        bind[Navigator].toInstance(navigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onPageLoad(NormalMode)(FakeRequest())

        status(result) shouldBe OK
        contentAsString(result) shouldBe viewAsString(app, None)
      }

    }

    "return Ok and the correct view when addresses are supplied" in {

      running(_.overrides(
        bind[Navigator].toInstance(navigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onPageLoad(NormalMode)(FakeRequest())

        status(result) shouldBe OK
        contentAsString(result) shouldBe viewAsString(app, None)
      }

    }

  }

  "post" must {

    "redirect to the page specified by the navigator following submission of valid data" in {
      val onwardRoute = controllers.routes.IndexController.onPageLoad()
      running(_.overrides(
        bind[Navigator].qualifiedWith(classOf[AcceptInvitation]).toInstance(new FakeNavigator(onwardRoute)),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction),
        bind[UserAnswersCacheConnector].toInstance(FakeUserAnswersCacheConnector)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onSubmit(NormalMode)(FakeRequest().withFormUrlEncodedBody("value" -> "1"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }

    }

    "save the user answer on submission of valid data" in {

      running(_.overrides(
        bind[Navigator].toInstance(navigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction),
        bind[UserAnswersCacheConnector].toInstance(FakeUserAnswersCacheConnector)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onSubmit(NormalMode)(FakeRequest().withFormUrlEncodedBody("value" -> "1"))

        status(result) shouldBe SEE_OTHER
        FakeUserAnswersCacheConnector.verify(AdviserAddressListId, addresses(1))
      }

    }

    "delete any existing address on submission of valid data" in {

      running(_.overrides(
        bind[Navigator].toInstance(navigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction),
        bind[UserAnswersCacheConnector].toInstance(FakeUserAnswersCacheConnector)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onSubmit(NormalMode)(FakeRequest().withFormUrlEncodedBody("value"->"1"))

        status(result) shouldBe SEE_OTHER
        FakeUserAnswersCacheConnector.verifyNot(AdviserAddressId)
      }

    }

    "return Bad Request and the correct view on submission of invalid data" in {

      running(_.overrides(
        bind[Navigator].toInstance(navigator),
        bind[AuthAction].toInstance(FakeAuthAction()),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction),
        bind[UserAnswersCacheConnector].toInstance(FakeUserAnswersCacheConnector)
      )) { app =>
        val controller = app.injector.instanceOf[PensionAdviserAddressListController]
        val result = controller.onSubmit(NormalMode)(FakeRequest().withFormUrlEncodedBody("value" -> "-1"))

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldBe viewAsString(app, Some(-1))
      }

    }

  }

}

object PensionAdviserAddressListControllerSpec {

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
      Some("GB")
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

    val view = app.injector.instanceOf[pension_adviser_address_list]

    view(form, addresses, NormalMode)(request, messages).toString()

  }

  def navigator = new FakeNavigator(controllers.routes.IndexController.onPageLoad())

}
