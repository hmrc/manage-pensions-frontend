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

package controllers.invitations.psa

import controllers.actions.{AuthAction, DataRetrievalAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import controllers.invitations.psa.routes.{AdviserDetailsController, CheckPensionAdviserAnswersController}
import models.CheckMode
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, Call}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.countryOptions.CountryOptions
import utils.{CheckYourAnswersFactory, UserAnswers, UserAnswerOps}
import views.html.check_your_answers

class CheckPensionAdviserAnswersControllerSpec
  extends ControllerWithNormalPageBehaviours with MockitoSugar {

  private val adviserName = "test name"
  private val userAnswer = UserAnswers()
    .adviserName(adviserName)
    .dataRetrievalAction

  private val countryOptions = new CountryOptions(environment, frontendAppConfig)

  private val checkYourAnswersFactory = new CheckYourAnswersFactory(countryOptions)

  def call: Call = CheckPensionAdviserAnswersController.onSubmit()

  private val view = injector.instanceOf[check_your_answers]

  private val adviserLabel = "messages__check__your__answer__adviser__name__label"
  private val siteChange = "site.change"

  val sections: Seq[SummaryListRow] = Seq(
    SummaryListRow(
      key = Key(Text(messages(adviserLabel)), classes = "govuk-!-width-one-half"),
      value = Value(Text(adviserName)),
      actions = Some(Actions("", items = Seq(
        ActionItem(
          href = AdviserDetailsController.onPageLoad(CheckMode).url,
          content = Text(messages(siteChange)),
          visuallyHiddenText = Some(messages(adviserLabel))
        )
      )))
    )
  )

  def viewAsString(): String =
    view(sections, None, call)(using fakeRequest, messages).toString

  def onPageLoadAction(
                        dataRetrievalAction: DataRetrievalAction,
                        fakeAuth: AuthAction
                      ): Action[AnyContent] = {
    new CheckPensionAdviserAnswersController(
      messagesApi, fakeAuth, navigator,
      dataRetrievalAction, requiredDateAction, checkYourAnswersFactory,
      controllerComponents, view
    ).onPageLoad()
  }

  def onSubmitAction(
                      dataRetrievalAction: DataRetrievalAction,
                      fakeAuth: AuthAction
                    ): Action[AnyContent] = {
    new CheckPensionAdviserAnswersController(
      messagesApi, fakeAuth, navigator,
      dataRetrievalAction, requiredDateAction, checkYourAnswersFactory,
      controllerComponents, view
    ).onSubmit()
  }

  behave like controllerWithOnPageLoadMethod(
    onPageLoadAction, getEmptyData, Some(userAnswer), () => viewAsString()
  )

  behave like controllerWithOnSubmitMethod(
    onSubmitAction, getEmptyData, None, None
  )
}
