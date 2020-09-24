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

package controllers.invitations.psp

import connectors.FakeUserAnswersCacheConnector
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.psp.PspNameFormProvider
import identifiers.invitations.psp.PspNameId
import models.NormalMode
import play.api.data.Form
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.{UserAnswers, _}
import views.html.invitations.psp.pspName

class PspNameControllerSpec extends ControllerWithQuestionPageBehaviours {

  private val formProvider = new PspNameFormProvider()
  private val form = formProvider()
  private val userAnswer = UserAnswers().set(PspNameId)("xyz").asOpt.value
  private val postRequest = fakeRequest.withJsonBody(userAnswer.json)
  private val pspNameView = injector.instanceOf[pspName]

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PspNameController(
      frontendAppConfig, messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, formProvider, stubMessagesControllerComponents(), pspNameView).onPageLoad(NormalMode)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PspNameController(
      frontendAppConfig, messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, formProvider, stubMessagesControllerComponents(), pspNameView).onSubmit(NormalMode)
  }

  private def viewAsString(form: Form[_]): String = pspNameView(form, NormalMode)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, userAnswer.dataRetrievalAction, form, form.fill("xyz"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData, form.bind(Map("pspName" -> "")), viewAsString, postRequest)

}

