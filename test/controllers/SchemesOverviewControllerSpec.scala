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

import config._
import connectors.UserAnswersCacheConnector
import controllers.actions.{DataRetrievalAction, _}
import controllers.routes.ListSchemesController
import models.Link
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsString, _}
import play.twirl.api.Html
import services.SchemesOverviewService
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import viewmodels.{CardViewModel, Message}
import views.html.schemesOverview

import scala.concurrent.Future

class SchemesOverviewControllerSpec extends ControllerSpecBase with MockitoSugar with BeforeAndAfterEach {

  import SchemesOverviewControllerSpec._

  val fakeSchemesOverviewService: SchemesOverviewService = mock[SchemesOverviewService]
  val fakeUserAnswersCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  private val view: schemesOverview = app.injector.instanceOf[schemesOverview]

  def controller(dataRetrievalAction: DataRetrievalAction = dontGetAnyData): SchemesOverviewController =
    new SchemesOverviewController(appConfig, messagesApi, fakeSchemesOverviewService, FakeAuthAction(),
      dataRetrievalAction, fakeUserAnswersCacheConnector, stubMessagesControllerComponents(),
      view)

  def viewAsString(): String = view(
    psaName,
    tiles
  )(fakeRequest, messages).toString


  "SchemesOverview Controller" when {
    "onPageLoad" must {
      "return OK and the correct tiles" in {
        when(fakeSchemesOverviewService.getTiles(eqTo(psaId))(any(), any(), any())).thenReturn(Future.successful(Seq(adminCard, schemeCard)))
        when(fakeSchemesOverviewService.getPsaName(eqTo(psaId))(any()))
          .thenReturn(Future.successful(Some(psaName)))
        when(fakeUserAnswersCacheConnector.save(any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(Json.obj()))

        val result = controller().onPageLoad(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString()
      }


    }

  "onRedirect" must {

    "redirect to overview page" in {

      val result = controller().redirect(fakeRequest)

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe controllers.routes.SchemesOverviewController.onPageLoad().url
          }
      }
    }
}

object SchemesOverviewControllerSpec extends ControllerSpecBase {
  val schemeName = "Test Scheme Name"
  val psaName = "Test Psa Name"
  private val formatter = DateTimeFormat.forPattern("dd MMMM YYYY")
  private val psaId = "A0000000"
  val html: Html = Html("test-html")
  val deleteDate: String = DateTime.now(DateTimeZone.UTC).plusDays(frontendAppConfig.daysDataSaved).toString(formatter)

  private val adminCard = CardViewModel(
    id = "administrator-card",
    heading = Message("messages__schemeOverview__psa_heading"),
    subHeading = Some(Message("messages__schemeOverview__psa_id")),
    subHeadingParam = Some(psaId),
    links = Seq(
      Link("psaLink", frontendAppConfig.registeredPsaDetailsUrl, Message("messages__schemeOverview__psa_change")),
      Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
        Message("messages__schemeOverview__psa_view_invitations")
      ),
      Link("deregister-link", frontendAppConfig.psaDeregisterUrl,
        Message("messages__schemeOverview__psa_deregister"))
    ))

  private val schemeCard = CardViewModel(
    id = "scheme-card",
    heading = Message("messages__schemeOverview__scheme_heading"),
    links = Seq(
      Link("view-schemes", ListSchemesController.onPageLoad().url, Message("messages__schemeOverview__scheme_view"))
    ),
    html = Some(html)
  )

  private val tiles = Seq(adminCard, schemeCard)
}




