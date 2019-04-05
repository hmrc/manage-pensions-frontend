/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors._
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.remove.RemovalDateFormProvider
import identifiers.remove.RemovalDateId
import models.{PsaSchemeDetails, PsaToBeRemovedFromScheme}
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import testhelpers.CommonBuilders
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers
import views.html.remove.removalDate
import utils.DateHelper._

import scala.concurrent.{ExecutionContext, Future}

class RemovalDateControllerSpec extends ControllerWithQuestionPageBehaviours {

  import RemovalDateControllerSpec._

  private val formProvider: RemovalDateFormProvider = new RemovalDateFormProvider()
  private val form = formProvider

  def controller(dataRetrievalAction: DataRetrievalAction = data, fakeAuth: AuthAction = FakeAuthAction(),
                 userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) = new RemovalDateController(
    frontendAppConfig, messagesApi, userAnswersCacheConnector, navigator, fakeAuth, dataRetrievalAction,
    requiredDataAction, formProvider, fakeSchemeDetailsConnector, fakePsaRemovalConnector)

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {
    controller(dataRetrievalAction, fakeAuth).onPageLoad()
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {
    controller(dataRetrievalAction, fakeAuth).onSubmit()
  }

  private def onSaveAction(userAnswersConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) = {
    controller(userAnswersCacheConnector = userAnswersConnector).onSubmit()
  }

  private def viewAsString(form: Form[LocalDate]) =
    removalDate(frontendAppConfig, form, psaName, schemeName, srn, formatDate(associationDate))(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethodWithoutPrePopulation(onPageLoadAction,
    userAnswer.dataRetrievalAction, form(associationDate, frontendAppConfig.earliestDatePsaRemoval), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, data, form(associationDate, frontendAppConfig.earliestDatePsaRemoval).bind(dateKeys),
    viewAsString, postRequest, Some(emptyPostRequest))

  behave like controllerThatSavesUserAnswers(onSaveAction, postRequest, RemovalDateId, date)
}

object RemovalDateControllerSpec {
  private val associationDate = LocalDate.parse("2018-10-01")
  private val schemeName = "test scheme name"
  private val psaName = "test psa name"
  private val srn = "test srn"
  private val pstr = "test pstr"
  private val date = LocalDate.now()

  private val userAnswer = UserAnswers().schemeName(schemeName).psaName(psaName).srn(srn).pstr(pstr)
  private val data = userAnswer.dataRetrievalAction

  val day: Int = LocalDate.now().getDayOfMonth
  val month: Int = LocalDate.now().getMonthOfYear
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

  val fakeSchemeDetailsConnector: SchemeDetailsConnector = new SchemeDetailsConnector {
    override def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaSchemeDetails] =
      Future(CommonBuilders.psaSchemeDetailsResponse)
    override def getSchemeDetailsVariations(psaId: String,
                                            schemeIdType: String,
                                            idNumber: String)(implicit hc: HeaderCarrier,
                                                              ec: ExecutionContext): Future[UserAnswers] =
      Future.successful(UserAnswers())
  }

  val fakePsaRemovalConnector: PsaRemovalConnector = new PsaRemovalConnector {
    override def remove(psaToBeRemoved: PsaToBeRemovedFromScheme)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future(())
  }
}



