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

package controllers.invitations

import connectors.FakeUserAnswersCacheConnector
import controllers.actions.DataRetrievalAction
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.AdviserDetailsFormProvider
import identifiers.invitations.AdviserNameId
import models.NormalMode
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeNavigator
import utils.UserAnswers
import views.html.invitations.adviserDetails

class AdviserDetailsControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new AdviserDetailsFormProvider()
  val form: Form[String] = formProvider()
  val userAnswer: UserAnswers = UserAnswers().adviserName("test")
  val postRequest: FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(userAnswer.json)

  val view: adviserDetails = app.injector.instanceOf[adviserDetails]


  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new AdviserDetailsController(
      frontendAppConfig, messagesApi,fakeAuth, new FakeNavigator(onwardRoute), dataRetrievalAction,
      requiredDataAction, formProvider, FakeUserAnswersCacheConnector, stubMessagesControllerComponents(),
      view).onPageLoad(NormalMode)
  }


  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new AdviserDetailsController(
      frontendAppConfig, messagesApi, fakeAuth, navigator, dataRetrievalAction,
      requiredDataAction, formProvider, FakeUserAnswersCacheConnector, stubMessagesControllerComponents(),
      view).onSubmit(NormalMode)
  }

  private def viewAsString(form: Form[_]) = view(form, NormalMode)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, userAnswer.dataRetrievalAction, form, form.fill("test"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData, form.bind(Map(AdviserNameId.toString -> "")), viewAsString, postRequest)

}

