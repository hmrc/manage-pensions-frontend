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

import connectors.{InvitationsCacheConnector, ListOfSchemesConnector}
import controllers.actions.{AuthAction, FakeAuthAction}
import models.requests.AuthenticatedRequest
import models.{ListOfSchemes, SchemeDetail}
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import views.html.list_schemes

import scala.concurrent.{ExecutionContext, Future}

class ListSchemesControllerSpec extends ControllerSpecBase {

  import ListSchemesControllerSpec._

  "ListSchemesController" must {
    "return OK and the correct view when there are no schemes" in {
      val fixture = testFixture(this, psaIdNoSchemes)
      val result = fixture.controller.onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(this, emptySchemes)
    }

    "return OK and the correct view when there are schemes" in {
      val fixture = testFixture(this, psaIdWithSchemes)
      val result = fixture.controller.onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(this, fullSchemes)
    }
  }

}

trait TestFixture {
  def controller: ListSchemesController
}

object ListSchemesControllerSpec {

  val psaIdNoSchemes: String = "A0000001"
  val psaIdWithSchemes: String = "A0000002"

  val emptySchemes: List[SchemeDetail] = List.empty[SchemeDetail]
  val fullSchemes: List[SchemeDetail] =
    List(
      SchemeDetail(
        name = "scheme-0",
        referenceNumber = "srn-0",
        schemeStatus = "Open",
        openDate = None,
        pstr = Some("pstr-0"),
        relationShip = None,
        underAppeal = None
      ),
      SchemeDetail(
        name = "scheme-1",
        referenceNumber = "srn-1",
        schemeStatus = "Deregistered",
        openDate = None,
        pstr = Some("pstr-1"),
        relationShip = None,
        underAppeal = None
      )
    )

  def testFixture(app: ControllerSpecBase, psaId: String): TestFixture = new TestFixture with MockitoSugar {

    private def authAction(psaId: String): AuthAction = FakeAuthAction.createWithPsaId(psaId)

    private def mockInvitationsCacheConnector(): InvitationsCacheConnector = mock[InvitationsCacheConnector]
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
        app.frontendAppConfig,
        app.messagesApi,
        authAction(psaId),
        listSchemesConnector(),
        mockInvitationsCacheConnector
      )

  }

  def viewAsString(app: ControllerSpecBase, schemes: List[SchemeDetail]): String =
    list_schemes(app.frontendAppConfig, schemes, false)(app.fakeRequest, app.messages).toString()

}
