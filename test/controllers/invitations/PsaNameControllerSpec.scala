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
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.PsaNameFormProvider
import models.NormalMode
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers
import utils._
import views.html.invitations.psaName

class PsaNameControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new PsaNameFormProvider()
  val form = formProvider()
  private val userAnswer = UserAnswers().inviteeName("xyz")
  private val postRequest = fakeRequest.withJsonBody(userAnswer.json)
  private val psaNameView = injector.instanceOf[psaName]

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PsaNameController(
      frontendAppConfig, messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, requiredDataAction, formProvider, stubMessagesControllerComponents(), psaNameView).onPageLoad(NormalMode)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PsaNameController(
      frontendAppConfig, messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, requiredDataAction, formProvider, stubMessagesControllerComponents(), psaNameView).onSubmit(NormalMode)
  }

  private def viewAsString(form: Form[_]): String = psaNameView(form, NormalMode)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, userAnswer.dataRetrievalAction, form, form.fill("xyz"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData, form.bind(Map("psaName" -> "")), viewAsString, postRequest)

}

