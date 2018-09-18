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

package controllers

import connectors._
import controllers.actions.{DataRetrievalAction, _}
import models.Scheme
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.{contentAsString, _}
import testhelpers.CommonBuilders
import views.html.schemeDetails

import scala.concurrent.Future

class SchemeDetailsControllerSpec extends ControllerSpecBase {
  import SchemeDetailsControllerSpec._

  "SchemeDetailsController" must {
    "return OK and the correct view for a GET" in {
      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(CommonBuilders.schemeDetailsWithPsaOnlyModel))
      when(fakeManagePensionsCacheConnector.fetch(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(manageSessionData)))
      val result = controller().onPageLoad(1)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return redirect when there is no manage pensions data to fetch" in {
      reset(fakeSchemeDetailsConnector)
      when(fakeSchemeDetailsConnector.getSchemeDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(CommonBuilders.schemeDetailsWithPsaOnlyModel))
      when(fakeManagePensionsCacheConnector.fetch(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))
      val result = controller().onPageLoad(1)(fakeRequest)
      status(result) mustBe SEE_OTHER
    }
  }
}

private object SchemeDetailsControllerSpec extends ControllerSpecBase with MockitoSugar {

  val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  val fakeManagePensionsCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData): SchemeDetailsController =
    new SchemeDetailsController(frontendAppConfig,
      messagesApi,
      fakeManagePensionsCacheConnector,
      fakeSchemeDetailsConnector,
      FakeAuthAction,
      dataRetrievalAction)

  val schemeName = "Test Scheme Name"
  val administrators = Some(Seq("Taylor Middle Rayon", "Smith A Tony"))
  val openDate = Some("28 February 2018")
  val pstr = "P12345678"
  val date: LocalDate = LocalDate.parse("2018-02-28")

  val manageSessionData: JsObject = Json.obj(
    "schemes" -> Seq(Scheme(pstr, date))
  )

  def viewAsString(): String = schemeDetails(
    frontendAppConfig,
    CommonBuilders.schemeDetails.schemeName,
    openDate,
    administrators
  )(fakeRequest, messages).toString
}
