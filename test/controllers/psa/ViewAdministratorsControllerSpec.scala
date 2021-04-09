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

package controllers.psa

import connectors.scheme.SchemeDetailsConnector
import controllers.ControllerSpecBase
import controllers.actions._
import handlers.ErrorHandler
import identifiers.SchemeNameId
import identifiers.invitations.PSTRId
import models._
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import play.api.test.Helpers.{contentAsString, _}
import services.SchemeDetailsService
import utils.UserAnswers
import viewmodels.AssociatedPsa
import views.html.psa.viewAdministrators
import views.html.{error_template, error_template_page_not_found}

import scala.concurrent.Future

class ViewAdministratorsControllerSpec extends ControllerSpecBase with BeforeAndAfterEach {

  import ViewAdministratorsControllerSpec._

  val viewAdministratorsView: viewAdministrators = app.injector.instanceOf[viewAdministrators]
  val errorHandlerView: error_template = app.injector.instanceOf[error_template]
  val errorHandlerNotFoundView: error_template_page_not_found = app.injector.instanceOf[error_template_page_not_found]
  val errorHandler = new ErrorHandler(messagesApi, errorHandlerView, errorHandlerNotFoundView)


  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData): ViewAdministratorsController = {
    new ViewAdministratorsController(
      messagesApi,
      fakeSchemeDetailsConnector,
      FakeAuthAction,
      dataRetrievalAction,
      errorHandler,
      controllerComponents,
      schemeDetailsService,
      viewAdministratorsView
    )
  }

  override def beforeEach(): Unit = {
    reset(fakeSchemeDetailsConnector, schemeDetailsService)
  }

  "SchemeDetailsController" must {
    "return OK and the correct view for a GET and NO financial info html if status is NOT open" in {
      val desAnswers: UserAnswers = UserAnswers(desUserAnswers.json.as[JsObject] ++ Json.obj(
        "schemeStatus" -> "Rejected")
      )
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(desAnswers))
      when(schemeDetailsService.administratorsVariations(any(), any(), any())).thenReturn(administrators)

      val result = controller(new FakeDataRetrievalAction(Some(desAnswers.json))).onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK

      val expected = viewAdministratorsView(schemeName, administrators, srn, isSchemeOpen = false)(fakeRequest, messages).toString()
      contentAsString(result) mustBe expected
    }

    "return NOT_FOUND when PSA data is not returned by API (as we don't know who administers the scheme)" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(UserAnswers(Json.obj("psaDetails" -> JsArray()))))

      val result = controller(dontGetAnyData).onPageLoad(srn)(fakeRequest)
      status(result) mustBe NOT_FOUND
    }

    "return NOT_FOUND and the correct not found view when the selected scheme is not administered by the logged-in PSA" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(UserAnswers(Json.obj("schemeStatus" -> "Open",
          SchemeNameId.toString -> schemeName,
          "psaDetails" -> JsArray(Seq(
            Json.obj(
              "id" -> "A0000007",
              "organisationOrPartnershipName" -> "partnetship name 2",
              "relationshipDate" -> "2018-07-01"
            )))))))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe NOT_FOUND
      contentAsString(result).contains(messages("messages__pageNotFound404__heading")) mustBe true
    }
  }
}

private object ViewAdministratorsControllerSpec extends MockitoSugar {

  private val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val schemeDetailsService: SchemeDetailsService = mock[SchemeDetailsService]

  private val schemeName = "Benefits Scheme"
  private val pstr = Some("10000678RE")
  private val srn = SchemeReferenceNumber("S1000000456")

  private val administrators =
    Some(
      Seq(
        AssociatedPsa("Taylor Middle Rayon", canRemove = true),
        AssociatedPsa("Smith A Tony", canRemove = false)
      )
    )

  private val desUserAnswers = UserAnswers(Json.obj(
    PSTRId.toString -> pstr.get,
    "schemeStatus" -> "Open",
    SchemeNameId.toString -> schemeName,
    "pspDetails" -> JsArray(
      Seq(JsString("id"))
    ),
    "psaDetails" -> JsArray(
      Seq(
        Json.obj(
          "id" -> "A0000000",
          "organisationOrPartnershipName" -> "partnership name 2",
          "relationshipDate" -> "2018-07-01"
        ),
        Json.obj(
          "id" -> "A0000001",
          "individual" -> Json.obj(
            "firstName" -> "Tony",
            "middleName" -> "A",
            "lastName" -> "Smith"
          ),
          "relationshipDate" -> "2018-07-01"
        )
      )
    )
  ))
}