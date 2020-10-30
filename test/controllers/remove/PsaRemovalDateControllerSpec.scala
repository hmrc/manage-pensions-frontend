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

package controllers.remove

import java.time.LocalDate

import connectors.admin.PsaRemovalConnector
import connectors.scheme.PensionSchemeVarianceLockConnector
import connectors.scheme.UpdateSchemeCacheConnector
import connectors.FakeUserAnswersCacheConnector
import connectors.UserAnswersCacheConnector
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.remove.RemovalDateFormProvider
import identifiers.remove.PsaRemovalDateId
import models.PsaToBeRemovedFromScheme
import models.SchemeVariance
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito._
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import play.api.test.Helpers.status
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.DateHelper._
import utils.UserAnswers
import views.html.remove.removalDate

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PsaRemovalDateControllerSpec extends ControllerWithQuestionPageBehaviours with MockitoSugar with BeforeAndAfterEach{

  import PsaRemovalDateControllerSpec._

  private val formProvider: RemovalDateFormProvider = new RemovalDateFormProvider()
  private val form = formProvider

  private val view = app.injector.instanceOf[removalDate]

  def controller(dataRetrievalAction: DataRetrievalAction = data, fakeAuth: AuthAction = FakeAuthAction,
                 userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) = new PsaRemovalDateController(
    frontendAppConfig, messagesApi, userAnswersCacheConnector, navigator, fakeAuth, dataRetrievalAction,
    requiredDataAction, formProvider, fakePsaRemovalConnector,
    mockedUpdateSchemeCacheConnector, mockedPensionSchemeVarianceLockConnector, stubMessagesControllerComponents(), view)

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {
    controller(dataRetrievalAction, fakeAuth).onPageLoad()
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {
    controller(dataRetrievalAction, fakeAuth).onSubmit()
  }

  private def onSaveAction(userAnswersConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) = {
    controller(userAnswersCacheConnector = userAnswersConnector).onSubmit()
  }

  private def viewAsString(form: Form[LocalDate]): String =
    view(form, psaName, schemeName, srn, formatDate(associationDate))(fakeRequest, messages).toString
  private def viewAsStringPostRequest(form: Form[LocalDate]): String =
    view(form, psaName, schemeName, srn, formatDate(associationDate))(postRequest, messages).toString

  override def beforeEach(): Unit = {
    reset(mockedPensionSchemeVarianceLockConnector)
    reset(mockedUpdateSchemeCacheConnector)
    when(mockedPensionSchemeVarianceLockConnector.getLockByPsa(any())(any(),any())).thenReturn(Future.successful(None))
  }

  behave like controllerWithOnPageLoadMethodWithoutPrePopulation(onPageLoadAction,
    userAnswer.dataRetrievalAction, form(associationDate, frontendAppConfig.earliestDatePsaRemoval), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, data, form(associationDate, frontendAppConfig.earliestDatePsaRemoval).bind(dateKeys),
    viewAsStringPostRequest, postRequest, Some(emptyPostRequest))

  behave like controllerThatSavesUserAnswers(onSaveAction, postRequest, PsaRemovalDateId, date)

  "controller" must {
    "remove lock and cached update data if present and lock and updated scheme owned by PSA" in {
      val sv = SchemeVariance(psaId = "A0000000", srn = srn)

      when(mockedPensionSchemeVarianceLockConnector.getLockByPsa(Matchers.eq("A0000000"))(any(),any())).thenReturn(Future.successful(Some(sv)))
      when(mockedPensionSchemeVarianceLockConnector.releaseLock(Matchers.eq("A0000000"), Matchers.eq(srn))(any(),any())).thenReturn(Future.successful(()))
      when(mockedUpdateSchemeCacheConnector.removeAll(Matchers.eq(srn))(any(),any())).thenReturn(Future.successful(Ok("")))

      val result = onSubmitAction(data, FakeAuthAction)(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)

      verify(mockedPensionSchemeVarianceLockConnector, times(1)).releaseLock(Matchers.eq("A0000000"), Matchers.eq(srn))(any(),any())
      verify(mockedUpdateSchemeCacheConnector, times(1)).removeAll(Matchers.eq(srn))(any(),any())
    }

    "NOT remove lock and cached update data if present and lock but DIFFERENT updated scheme owned by PSA" in {
      val anotherSrn = "test srn 2"
      val sv = SchemeVariance(psaId = "A0000000", srn = anotherSrn)

      when(mockedPensionSchemeVarianceLockConnector.getLockByPsa(Matchers.eq("A0000000"))(any(),any())).thenReturn(Future.successful(Some(sv)))

      val result = onSubmitAction(data, FakeAuthAction)(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)

      verify(mockedPensionSchemeVarianceLockConnector, times(0)).releaseLock(any(), any())(any(),any())
      verify(mockedUpdateSchemeCacheConnector, times(0)).removeAll(any())(any(),any())
    }
  }
}

object PsaRemovalDateControllerSpec extends MockitoSugar {
  private val associationDate = LocalDate.parse("2018-10-01")
  private val schemeName = "test scheme name"
  private val psaName = "test psa name"
  private val srn = "test srn"
  private val pstr = "test pstr"
  private val date = LocalDate.now()

  private val userAnswer = UserAnswers().schemeName(schemeName).psaName(psaName).srn(srn).pstr(pstr).associatedDate(associationDate)
  private val data = userAnswer.dataRetrievalAction

  val day: Int = LocalDate.now().getDayOfMonth
  val month: Int = LocalDate.now().getMonthValue
  val year: Int = LocalDate.now().getYear

  val dateKeys = Map("removalDate.day" -> "", "removalDate.month" -> "", "removalDate.year" -> "")


  val postRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj(
    "removalDate.day" -> day.toString,
    "removalDate.month" -> month.toString,
    "removalDate.year" -> year.toString)
  )

  val emptyPostRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(Json.obj(
    "removalDate.day" -> "",
    "removalDate.month" -> "",
    "removalDate.year" -> "")
  )

  val fakePsaRemovalConnector: PsaRemovalConnector = new PsaRemovalConnector {
    override def remove(psaToBeRemoved: PsaToBeRemovedFromScheme)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future(())
  }

  val mockedPensionSchemeVarianceLockConnector: PensionSchemeVarianceLockConnector =
    mock[PensionSchemeVarianceLockConnector]
  val mockedUpdateSchemeCacheConnector: UpdateSchemeCacheConnector = mock[UpdateSchemeCacheConnector]
}
