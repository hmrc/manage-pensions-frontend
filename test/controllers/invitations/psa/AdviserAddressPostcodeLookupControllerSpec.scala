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

package controllers.invitations.psa

import connectors.{AddressLookupConnector, FakeUserAnswersCacheConnector, UserAnswersCacheConnector}
import controllers.ControllerSpecBase
import controllers.actions.{AuthAction, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.invitations.psa.AdviserAddressPostcodeLookupFormProvider
import identifiers.invitations.psa.AdviserNameId
import models.TolerantAddress
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpException
import utils.annotations.AcceptInvitation
import utils.{FakeNavigator, Navigator}
import views.html.invitations.psa.adviserPostcode

import scala.concurrent.Future

class AdviserAddressPostcodeLookupControllerSpec
  extends WordSpec
    with MustMatchers
    with MockitoSugar
    with ScalaFutures
    with OptionValues {

  import AdviserAddressPostcodeLookupControllerSpec._

  def dataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj(
    AdviserNameId.toString -> name
  )))

  "get" must {
    "display the view" in {

      running(_.overrides(
        bind[Navigator].toInstance(FakeNavigator),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction),
        bind[AuthAction].toInstance(FakeAuthAction)
      )) {
        implicit app =>

          val controller = app.injector.instanceOf[AdviserAddressPostcodeLookupController]
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual OK
          contentAsString(result) mustEqual viewAsString(Some(postcode))
      }
    }
  }

  "post" must {

    "return a redirect on successful submission" in {

      val onwardRoute = controllers.routes.IndexController.onPageLoad()
      val addressConnector: AddressLookupConnector = mock[AddressLookupConnector]
      val cacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector

      when(addressConnector.addressLookupByPostCode(eqTo(postcode))(any(), any()))
        .thenReturn(Future.successful(Seq(address)))

      running(_.overrides(
        bind[Navigator].qualifiedWith(classOf[AcceptInvitation]).toInstance(new FakeNavigator(onwardRoute)),
        bind[UserAnswersCacheConnector].toInstance(cacheConnector),
        bind[AddressLookupConnector].toInstance(addressConnector),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction),
        bind[AuthAction].toInstance(FakeAuthAction)
      )) {
        app =>

          val controller = app.injector.instanceOf[AdviserAddressPostcodeLookupController]
          val result = controller.onSubmit()(FakeRequest().withFormUrlEncodedBody("value" -> postcode))

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "return a bad request" when {
      "the postcode look fails to return result" in {

        val addressConnector: AddressLookupConnector = mock[AddressLookupConnector]

        when(addressConnector.addressLookupByPostCode(eqTo(postcode))(any(), any())) thenReturn
          Future.failed(new HttpException("Failed", INTERNAL_SERVER_ERROR))

        running(_.overrides(
          bind[Navigator].toInstance(FakeNavigator),
          bind[UserAnswersCacheConnector].toInstance(FakeUserAnswersCacheConnector),
          bind[AddressLookupConnector].toInstance(addressConnector),
          bind[DataRetrievalAction].toInstance(dataRetrievalAction),
          bind[AuthAction].toInstance(FakeAuthAction)
        )) {
          implicit app =>

            val controller = app.injector.instanceOf[AdviserAddressPostcodeLookupController]
            val result = controller.onSubmit()(FakeRequest().withFormUrlEncodedBody("value" -> postcode))

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual viewAsString(Some(postcode), form.withError("value", "messages__error__postcode__lookup__invalid"))
        }
      }

      "the postcode is invalid" in {

        val invalidPostcode = "*" * 10

        val addressConnector: AddressLookupConnector = mock[AddressLookupConnector]

        verifyZeroInteractions(addressConnector)

        running(_.overrides(
          bind[Navigator].toInstance(FakeNavigator),
          bind[UserAnswersCacheConnector].toInstance(FakeUserAnswersCacheConnector),
          bind[AddressLookupConnector].toInstance(addressConnector),
          bind[DataRetrievalAction].toInstance(dataRetrievalAction),
          bind[AuthAction].toInstance(FakeAuthAction)
        )) {
          implicit app =>

            val controller = app.injector.instanceOf[AdviserAddressPostcodeLookupController]
            val result = controller.onSubmit()(FakeRequest().withFormUrlEncodedBody("value" -> invalidPostcode))
            val invalidForm = form.bind(Map("value" -> invalidPostcode))

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual viewAsString(Some(invalidPostcode), invalidForm)
        }
      }
    }

    "return ok when the postcode returns no results which presents with form errors" in {

      val addressConnector: AddressLookupConnector = mock[AddressLookupConnector]

      when(addressConnector.addressLookupByPostCode(eqTo(postcode))(any(), any()))
        .thenReturn(Future.successful(Seq.empty))

      running(_.overrides(
        bind[Navigator].toInstance(FakeNavigator),
        bind[UserAnswersCacheConnector].toInstance(FakeUserAnswersCacheConnector),
        bind[AddressLookupConnector].toInstance(addressConnector),
        bind[DataRetrievalAction].toInstance(dataRetrievalAction),
        bind[AuthAction].toInstance(FakeAuthAction)
      )) {
        implicit app =>

          val controller = app.injector.instanceOf[AdviserAddressPostcodeLookupController]
          val result = controller.onSubmit()(FakeRequest().withFormUrlEncodedBody("value" -> postcode))

          status(result) mustEqual OK
          contentAsString(result) mustEqual viewAsString(Some(postcode), form.withError("value", "messages__error__postcode__lookup__no__results"))
      }
    }
  }
}

object AdviserAddressPostcodeLookupControllerSpec extends ControllerSpecBase {

  val postcode = "ZZ1 1ZZ"
  val name = "Pension Adviser"
  val form = new AdviserAddressPostcodeLookupFormProvider()()
  val view: adviserPostcode = app.injector.instanceOf[adviserPostcode]


  def viewAsString(value: Option[String] = Some(postcode), form: Form[String] = form)
                  (implicit app: Application): String = {

    val request = FakeRequest()
    val messages = app.injector.instanceOf[MessagesApi].preferred(request)

    view(form, name)(request, messages).toString()

  }

  val address = TolerantAddress(Some("Address 1"), Some("Address 2"), None, None, None, Some("GB"))

}
