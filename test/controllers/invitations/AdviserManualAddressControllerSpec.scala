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
import controllers.ControllerSpecBase
import controllers.actions.AuthAction
import controllers.actions.DataRetrievalAction
import controllers.actions.FakeAuthAction
import controllers.actions.FakeDataRetrievalAction
import forms.invitations.AdviserManualAddressFormProvider
import identifiers.invitations.AdviserAddressId
import identifiers.invitations.AdviserAddressListId
import identifiers.invitations.AdviserAddressPostCodeLookupId
import identifiers.invitations.AdviserNameId
import models.Address
import models.NormalMode
import models.TolerantAddress
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.MustMatchers
import org.scalatest.OptionValues
import org.scalatest.WordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.annotations.AcceptInvitation
import utils.countryOptions.CountryOptions
import utils.FakeCountryOptions
import utils.FakeNavigator
import utils.Navigator
import views.html.invitations.adviserAddress

class AdviserManualAddressControllerSpec extends WordSpec with MustMatchers with MockitoSugar with ScalaFutures with OptionValues {

  import AdviserManualAddressControllerSpec._

  def dataRetrieval(data: Option[JsValue] = Some(Json.obj())) = new FakeDataRetrievalAction(
    data.map( _.as[JsObject] ++ Json.obj(AdviserNameId.toString -> name) )
  )

  "get" must {
    "return OK with view" when {
      "data is not retrieved" in {

        running(_.overrides(
          bind[CountryOptions].to[FakeCountryOptions],
          bind[Navigator].qualifiedWith(classOf[AcceptInvitation]).to(FakeNavigator),
          bind[DataRetrievalAction].toInstance(dataRetrieval()),
          bind[AuthAction].toInstance(FakeAuthAction)
        )) {
          implicit app =>

            val form = app.injector.instanceOf[AdviserManualAddressFormProvider]
            val controller = app.injector.instanceOf[AdviserManualAddressController]
            val result = controller.onPageLoad(NormalMode, false)(FakeRequest())

            status(result) mustEqual OK
            contentAsString(result) mustEqual viewAsString(Some(address), form())

        }

      }

      "data is retrieved" in {
        running(_.overrides(
          bind[CountryOptions].to[FakeCountryOptions],
          bind[Navigator].qualifiedWith(classOf[AcceptInvitation]).to(FakeNavigator),
          bind[DataRetrievalAction].toInstance(dataRetrieval(Some(Json.obj(AdviserAddressId.toString -> address)))),
          bind[AuthAction].toInstance(FakeAuthAction)
        )) {
          implicit app =>

            val form = app.injector.instanceOf[AdviserManualAddressFormProvider]
            val controller = app.injector.instanceOf[AdviserManualAddressController]
            val result = controller.onPageLoad(NormalMode, true)(FakeRequest())

            status(result) mustEqual OK
            contentAsString(result) mustEqual viewAsString(Some(address), form().fill(address), true, "adviser__address__confirm")

        }
      }

      "data is not retrieved but there is a selected address" in {

        running(_.overrides(
          bind[CountryOptions].to[FakeCountryOptions],
          bind[Navigator].qualifiedWith(classOf[AcceptInvitation]).to(FakeNavigator),
          bind[DataRetrievalAction].toInstance(dataRetrieval(Some(Json.obj(AdviserAddressListId.toString -> tolerantAddress)))),
          bind[AuthAction].toInstance(FakeAuthAction)
        )) {
          implicit app =>

            val form = app.injector.instanceOf[AdviserManualAddressFormProvider]
            val controller = app.injector.instanceOf[AdviserManualAddressController]
            val result = controller.onPageLoad(NormalMode, true)(FakeRequest())

            status(result) mustEqual OK
            contentAsString(result) mustEqual viewAsString(Some(address), form().fill(tolerantAddress.toAddress), true, "adviser__address__confirm")

        }

      }

    }
  }

  "post" must {

    "redirect to the postCall on valid data request will save address to answers and remove address lookup ID" in {

        running(_.overrides(
          bind[CountryOptions].to[FakeCountryOptions],
          bind[UserAnswersCacheConnector].to(FakeUserAnswersCacheConnector),
          bind[Navigator].qualifiedWith(classOf[AcceptInvitation]).to(FakeNavigator),
          bind[DataRetrievalAction].toInstance(dataRetrieval()),
          bind[AuthAction].toInstance(FakeAuthAction)
        )) {
          app =>

            val controller = app.injector.instanceOf[AdviserManualAddressController]
            val result = controller.onSubmit(NormalMode, false)(FakeRequest().withFormUrlEncodedBody(
              ("addressLine1", "value 1"),
              ("addressLine2", "value 2"),
              ("postCode", "AB1 1AB"),
              "country" -> "GB")
            )

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).get mustEqual FakeNavigator.desiredRoute.url

            val address = Address("value 1", "value 2", None, None, Some("AB1 1AB"), "GB")

            FakeUserAnswersCacheConnector.verify(AdviserAddressId, address)
            FakeUserAnswersCacheConnector.verifyRemoved(AdviserAddressPostCodeLookupId)
        }

      }

    "return BAD_REQUEST with view on invalid data request" in {

      running(_.overrides(
        bind[CountryOptions].to[FakeCountryOptions],
        bind[Navigator].qualifiedWith(classOf[AcceptInvitation]).to(FakeNavigator),
        bind[DataRetrievalAction].toInstance(dataRetrieval()),
        bind[AuthAction].toInstance(FakeAuthAction)
      )) {
        implicit app =>

          val form = app.injector.instanceOf[AdviserManualAddressFormProvider]
          val controller = app.injector.instanceOf[AdviserManualAddressController]
          val result = controller.onSubmit(NormalMode, false)(FakeRequest().withFormUrlEncodedBody())

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual viewAsString(None, form().bind(Map.empty[String, String]))
      }

    }

  }

}

object AdviserManualAddressControllerSpec extends ControllerSpecBase {

  val name = "Pension Adviser"

  val tolerantAddress = TolerantAddress(
    Some("address line 1"),
    Some("address line 2"),
    Some("address line 3"),
    Some("address line 4"),
    Some("ZZ11ZZ"),
    Some("GB")
  )

  val address = tolerantAddress.toAddress

  val addressData: Map[String, String] = Map(
    "addressLine1" -> "address line 1",
    "addressLine2" -> "address line 2",
    "addressLine3" -> "address line 3",
    "addressLine4" -> "address line 4",
    "postCode" -> "AB1 1AP",
    "country" -> "GB"
  )

  val messageKeyPrefix = "adviser__address"

  private val countryOptions = FakeCountryOptions.fakeCountries
  private val view = injector.instanceOf[adviserAddress]

  def viewAsString(value: Option[Address], form: Form[Address], prepopulated: Boolean = false, prefix: String = messageKeyPrefix)
                  (implicit app: Application): String = {

    val appConfig = app.injector.instanceOf[FrontendAppConfig]
    val messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

    view(form, NormalMode, countryOptions, prepopulated, prefix, name)(FakeRequest(), messages).toString()

  }

}
