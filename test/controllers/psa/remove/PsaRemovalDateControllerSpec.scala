/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.psa.remove

import connectors.admin.PsaRemovalConnector
import connectors.scheme.{PensionSchemeVarianceLockConnector, UpdateSchemeCacheConnector}
import connectors.{FakeUserAnswersCacheConnector, UserAnswersCacheConnector}
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.psa.remove.RemovalDateFormProvider
import identifiers.psa.ListOfPSADetailsId
import identifiers.psa.remove.PsaRemovalDateId
import models.{SchemeReferenceNumber, SchemeVariance}
import models.psa.remove.PsaToBeRemovedFromScheme
import models.psa.{Name, PsaDetails}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito._
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status, _}
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateHelper._
import utils.{UserAnswers, UserAnswerOps}
import views.html.psa.remove.removalDate

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class PsaRemovalDateControllerSpec extends ControllerWithQuestionPageBehaviours with MockitoSugar with BeforeAndAfterEach {

  import PsaRemovalDateControllerSpec._

  private val formProvider: RemovalDateFormProvider = new RemovalDateFormProvider()
  private val form = formProvider

  private val view = app.injector.instanceOf[removalDate]

  def controller(dataRetrievalAction: DataRetrievalAction = data, fakeAuth: AuthAction = FakeAuthAction,
                 userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) = new PsaRemovalDateController(
    frontendAppConfig, messagesApi, userAnswersCacheConnector, navigator, fakeAuth, dataRetrievalAction,
    requiredDataAction, formProvider, fakePsaRemovalConnector,
    mockedUpdateSchemeCacheConnector, mockedPensionSchemeVarianceLockConnector, controllerComponents, view, fakePsaSchemeAuthAction)

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {
    controller(dataRetrievalAction, fakeAuth).onSubmit(srn)
  }

  private def onSaveAction(userAnswersConnector: UserAnswersCacheConnector) = {
    controller(userAnswersCacheConnector = userAnswersConnector).onSubmit(srn)
  }

  private def viewAsStringPostRequest(form: Form[LocalDate]): String =
    view(form, psaName, schemeName, srn, formatDate(associationDate))(using postRequest, messages).toString

  override def beforeEach(): Unit = {
    reset(mockedPensionSchemeVarianceLockConnector)
    reset(mockedUpdateSchemeCacheConnector)
    when(mockedPensionSchemeVarianceLockConnector.getLockByPsa(any())(using any(), any())).thenReturn(Future.successful(None))
  }

  behave like controllerWithOnSubmitMethod(onSubmitAction, data, form(associationDate, frontendAppConfig.earliestDatePsaRemoval).bind(dateKeys),
    viewAsStringPostRequest, postRequest, Some(emptyPostRequest))

  behave like controllerThatSavesUserAnswers(onSaveAction, postRequest, PsaRemovalDateId, date)

  "controller" must {
    "remove lock and cached update data if present and lock and updated scheme owned by PSA" in {
      val sv = SchemeVariance(psaId = "A0000000", srn = srn)

      when(mockedPensionSchemeVarianceLockConnector.getLockByPsa(ArgumentMatchers.eq("A0000000"))(using any(), any())).thenReturn(Future.successful(Some(sv)))
      when(mockedUpdateSchemeCacheConnector.removeAll(ArgumentMatchers.eq(srn))(using any(), any())).thenReturn(Future.successful(Ok("")))

      val result = onSubmitAction(data, FakeAuthAction)(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)

      verify(mockedUpdateSchemeCacheConnector, times(1)).removeAll(any())(using any(), any())
      verify(mockedPensionSchemeVarianceLockConnector, times(1)).
        releaseLock(ArgumentMatchers.eq("A0000000"), ArgumentMatchers.eq(srn))(using any(), any())
      verify(mockedUpdateSchemeCacheConnector, times(1)).removeAll(ArgumentMatchers.eq(srn))(using any(), any())
    }

    "NOT remove lock and cached update data if present and lock but DIFFERENT updated scheme owned by PSA" in {
      val anotherSrn = "test srn 2"
      val sv = SchemeVariance(psaId = "A0000000", srn = anotherSrn)

      when(mockedPensionSchemeVarianceLockConnector.getLockByPsa(ArgumentMatchers.eq("A0000000"))(using any(), any())).thenReturn(Future.successful(Some(sv)))

      val result = onSubmitAction(data, FakeAuthAction)(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)

      verify(mockedPensionSchemeVarianceLockConnector, times(0)).releaseLock(any(), any())(using any(), any())
      verify(mockedUpdateSchemeCacheConnector, times(0)).removeAll(any())(using any(), any())
    }

    "return the correct relationship start date" in {
      val result = controller(userAnswer.dataRetrievalAction, FakeAuthAction).onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      val fm = form(associationDate, frontendAppConfig.earliestDatePsaRemoval)
      contentAsString(result) mustBe view(fm, psaName, schemeName, srn, formatDate(relationshipDateAsLocalDate))(using fakeRequest, messages).toString
    }
  }
}

object PsaRemovalDateControllerSpec extends MockitoSugar {
  private val associationDate = LocalDate.parse("2018-10-01")
  private val relationshipDate = "2018-11-01"
  private val relationshipDateAsLocalDate: LocalDate = LocalDate.parse("2018-11-01")
  private val schemeName = "test scheme name"
  private val psaName = "test psa name"
  val srn: SchemeReferenceNumber = SchemeReferenceNumber("AB123456C")
  private val pstr = "test pstr"
  private val date = LocalDate.now()

  private val psaDetails: PsaDetails = PsaDetails(
    "A0000000",
    Some("partnership name"),
    Some(Name(Some("Taylor"),
      Some("Middle"),
      Some("Rayon"))),
    Some(relationshipDate)
  )

  private val listOfPSADetails: List[PsaDetails] = List(psaDetails)

  private val userAnswer = UserAnswers()
    .schemeName(schemeName)
    .psaName(psaName)
    .srn(srn)
    .pstr(pstr)
    .associatedDate(associationDate)
    .set(ListOfPSADetailsId)(listOfPSADetails).getOrElse(UserAnswers())

  private val data = userAnswer.dataRetrievalAction

  val day: Int = LocalDate.now().getDayOfMonth
  val month: Int = LocalDate.now().getMonthValue
  val year: Int = LocalDate.now().getYear

  val dateKeys: Map[String, String] = Map("removalDate.day" -> "", "removalDate.month" -> "", "removalDate.year" -> "")

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
    override def remove(srn: String, psaToBeRemoved: PsaToBeRemovedFromScheme
                       )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future(())
  }

  val mockedPensionSchemeVarianceLockConnector: PensionSchemeVarianceLockConnector =
    mock[PensionSchemeVarianceLockConnector]
  val mockedUpdateSchemeCacheConnector: UpdateSchemeCacheConnector = mock[UpdateSchemeCacheConnector]
}
