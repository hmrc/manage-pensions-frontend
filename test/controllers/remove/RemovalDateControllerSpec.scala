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

package controllers.remove

import connectors.{FakeUserAnswersCacheConnector, UserAnswersCacheConnector}
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.remove.RemovalDateFormProvider
import identifiers.invitations.RemoveAsSchemeAdministratorId
import identifiers.remove.RemovalDateId
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.FakeRequest
import utils.UserAnswers
import views.html.remove.removalDate

class RemovalDateControllerSpec extends ControllerWithQuestionPageBehaviours {

  import RemovalDateControllerSpec._

  def controller(dataRetrievalAction: DataRetrievalAction = data, fakeAuth: AuthAction = FakeAuthAction(),
                 userAnswersCacheConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) = new RemovalDateController(
    frontendAppConfig, messagesApi, userAnswersCacheConnector, navigator, fakeAuth,
    dataRetrievalAction, requiredDataAction, formProvider)

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {
    controller(dataRetrievalAction, fakeAuth).onPageLoad()
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {
    controller(dataRetrievalAction, fakeAuth).onSubmit()
  }

  private def onSaveAction(userAnswersConnector: UserAnswersCacheConnector = FakeUserAnswersCacheConnector) = {
    controller(userAnswersCacheConnector = userAnswersConnector).onSubmit()
  }

  private def viewAsString(form: Form[LocalDate] = form) = removalDate(frontendAppConfig, form, psaName, schemeName, srn)(fakeRequest, messages).toString

  behave like controllerWithOnPageLoadMethodWithoutPrePopulation(onPageLoadAction,
    userAnswer.dataRetrievalAction, form, viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, data, form.bind(
    Map(
      "removalDate.day" -> "",
      "removalDate.month" -> "",
      "removalDate.year" -> "")), viewAsString, postRequest)

  behave like controllerThatSavesUserAnswers(onSaveAction, postRequest, RemovalDateId, date)
}

object RemovalDateControllerSpec {
  private val formProvider = new RemovalDateFormProvider()
  private val form = formProvider()
  private val schemeName = "test scheme name"
  private val psaName = "test psa name"
  private val srn = "test srn"
  private val date = LocalDate.now().minusYears(1)

  private val userAnswer = UserAnswers().schemeName(schemeName).psaName(psaName).srn(srn)
  private val data = userAnswer.dataRetrievalAction
  private val validData = userAnswer.removalDate(date).dataRetrievalAction

  val day: Int = LocalDate.now().getDayOfMonth
  val month: Int = LocalDate.now().getMonthOfYear
  val year: Int = LocalDate.now().getYear - 1


  val postRequest = FakeRequest().withJsonBody(Json.obj(
    "removalDate.day" -> day.toString,
    "removalDate.month" -> month.toString,
    "removalDate.year" -> year.toString)
  )
}



