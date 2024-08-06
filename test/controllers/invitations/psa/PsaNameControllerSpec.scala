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

import connectors.FakeUserAnswersCacheConnector
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.psa.PsaNameFormProvider
import models.NormalMode
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import utils.{UserAnswers, _}
import views.html.invitations.psa.psaName

class PsaNameControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new PsaNameFormProvider()
  val form = formProvider()
  private val userAnswer = UserAnswers().inviteeName("xyz")
  private val postRequest = fakeRequest.withJsonBody(userAnswer.json)
  private val psaNameView = injector.instanceOf[psaName]

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PsaNameController(
      messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, formProvider, controllerComponents, psaNameView, fakePsaSchemeAuthAction).onPageLoad(NormalMode, srn)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new PsaNameController(
      messagesApi, FakeUserAnswersCacheConnector, navigator, fakeAuth,
      dataRetrievalAction, formProvider, controllerComponents, psaNameView, fakePsaSchemeAuthAction).onSubmit(NormalMode, srn)
  }

  private def viewAsString(form: Form[_]): String = psaNameView(form, NormalMode, srn)(fakeRequest, messages).toString


  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, userAnswer.dataRetrievalAction, form, form.fill("xyz"), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData, form.bind(Json.obj(), 0), viewAsString, postRequest)

}

