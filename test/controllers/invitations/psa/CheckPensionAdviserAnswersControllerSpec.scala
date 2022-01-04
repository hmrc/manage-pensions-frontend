/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.actions.{AuthAction, DataRetrievalAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.invitations.psa.routes.{AdviserDetailsController, CheckPensionAdviserAnswersController}
import models.CheckMode
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import utils.{CheckYourAnswersFactory, UserAnswers}
import utils.countryOptions.CountryOptions
import viewmodels.{AnswerRow, AnswerSection}
import views.html.check_your_answers

class CheckPensionAdviserAnswersControllerSpec extends ControllerWithNormalPageBehaviours with MockitoSugar{
  //scalastyle:off magic.number
  private val adviserName = "test name"
  private val userAnswer = UserAnswers()
    .adviserName(adviserName)
    .dataRetrievalAction

  private val countryOptions = new CountryOptions(environment, frontendAppConfig)

  private val checkYourAnswersFactory = new CheckYourAnswersFactory(countryOptions)

  def call: Call = CheckPensionAdviserAnswersController.onSubmit()
  private val view = injector.instanceOf[check_your_answers]

  val sections = Seq(AnswerSection(
    None,
    Seq(AnswerRow(
      "messages__check__your__answer__adviser__name__label",
      Seq(adviserName),
      true,
      Some(AdviserDetailsController.onPageLoad(CheckMode).url)
    ))
  ))

  def viewAsString() = view(sections, None, call)(fakeRequest, messages).toString

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new CheckPensionAdviserAnswersController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction, requiredDateAction,
      checkYourAnswersFactory, controllerComponents, view).onPageLoad()
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new CheckPensionAdviserAnswersController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction, requiredDateAction,
      checkYourAnswersFactory, controllerComponents, view).onSubmit()
  }

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, Some(userAnswer), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData,  None, None)
}
