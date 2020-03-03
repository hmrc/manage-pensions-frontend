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

package controllers

import connectors.admin.MinimalPsaConnector
import connectors.FakeUserAnswersCacheConnector
import connectors.scheme.ListOfSchemesConnector
import controllers.actions.{AuthAction, FakeAuthAction}
import models.{ListOfSchemes, SchemeDetail, SchemeStatus}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.list_schemes

import scala.concurrent.{ExecutionContext, Future}


class ListSchemesControllerSpec extends ControllerSpecBase {

  import ListSchemesControllerSpec._

  "ListSchemesController" when {

    when(mockMinimalPsaConnector.getPsaNameFromPsaID(any())(any(), any())).thenReturn(Future.successful(Some(psaName)))

    "return OK and the correct view when there are no schemes" in {
      val fixture = testFixture(psaIdNoSchemes)

      val result = fixture.controller.onPageLoad(fakeRequest)

      status(result) mustBe OK

      contentAsString(result) mustBe viewAsString(
        schemes = emptySchemes,
        numberOfSchemes = emptySchemes.length,
        pagination = 1,
        currentPage = 1,
        pageNumberLinks = Seq.empty
      )
    }

    "return OK and the correct view when there are schemes without pagination" in {
      val fixture = testFixture(psaIdWithSchemes)

      val result = fixture.controller.onPageLoad(fakeRequest)

      status(result) mustBe OK

      contentAsString(result) mustBe viewAsString(
        schemes = fullSchemes,
        numberOfSchemes = fullSchemes.length,
        pagination = 5,
        currentPage = 1,
        pageNumberLinks = Seq(1, 2)
      )
    }
  }
}

trait TestFixture {
  def controller: ListSchemesController
}

object ListSchemesControllerSpec extends ControllerSpecBase with MockitoSugar {
  val psaIdNoSchemes: String = "A0000001"
  val psaIdWithSchemes: String = "A0000002"
  val psaName: String = "Test Psa Name"
  val emptySchemes: List[SchemeDetail] = List.empty[SchemeDetail]

  val fullSchemes: List[SchemeDetail] =
    List(
      SchemeDetail(
        name = "scheme-0",
        referenceNumber = "srn-0",
        schemeStatus = SchemeStatus.Open.value,
        openDate = None,
        pstr = Some("pstr-0"),
        relationShip = None,
        underAppeal = None
      ),
      SchemeDetail(
        name = "scheme-1",
        referenceNumber = "srn-1",
        schemeStatus = SchemeStatus.Deregistered.value,
        openDate = None,
        pstr = Some("pstr-1"),
        relationShip = None,
        underAppeal = None
      )
    )

  def testFixture(psaId: String): TestFixture = new TestFixture with MockitoSugar {

    private def authAction(psaId: String): AuthAction = FakeAuthAction.createWithPsaId(psaId)

    private def listSchemesConnector(): ListOfSchemesConnector = new ListOfSchemesConnector {

      override def getListOfSchemes(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ListOfSchemes] = {
        psaId match {
          case `psaIdNoSchemes` =>
            Future.successful(
              ListOfSchemes(
                "test-processing-date",
                "test-total-schemes-registered",
                None
              )
            )
          case `psaIdWithSchemes` =>
            Future.successful(
              ListOfSchemes(
                "test-processing-date",
                "test-total-schemes-registered",
                Some(fullSchemes)
              )
            )
          case _ =>
            Future.failed(new Exception(s"No stubbed response in ListOfSchemesConnector for PSA Id $psaId"))
        }
      }
    }

    override val controller: ListSchemesController =
      new ListSchemesController(
        frontendAppConfig,
        messagesApi,
        authAction(psaId),
        getDataWithPsaName(psaId),
        listSchemesConnector(),
        mockMinimalPsaConnector,
        FakeUserAnswersCacheConnector,
        stubMessagesControllerComponents(),
        view
      )
  }

  val view: list_schemes = app.injector.instanceOf[list_schemes]

  def viewAsString(schemes: List[SchemeDetail],
                   numberOfSchemes: Int,
                   pagination: Int,
                   currentPage: Int,
                   pageNumberLinks: Seq[Int]): String =
    view(
      schemes = schemes,
      psaName = psaName,
      numberOfSchemes = numberOfSchemes,
      pagination = pagination,
      currentPage = currentPage,
      pageNumberLinks = pageNumberLinks
    )(fakeRequest, messages).toString()

  val mockMinimalPsaConnector: MinimalPsaConnector = mock[MinimalPsaConnector]
}