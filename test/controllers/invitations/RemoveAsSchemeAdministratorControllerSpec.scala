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

package controllers.invitations

import connectors.{FakeUserAnswersCacheConnector, SchemeDetailsConnector}
import controllers.actions._
import controllers.behaviours.ControllerWithQuestionPageBehaviours
import forms.invitations.RemoveAsSchemeAdministratorFormProvider
import models.PsaSchemeDetails
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testhelpers.CommonBuilders
import uk.gov.hmrc.http.HeaderCarrier
import utils.{UserAnswerOps, UserAnswers}
import views.html.invitations.removeAsSchemeAdministrator

import scala.concurrent.{ExecutionContext, Future}

class RemoveAsSchemeAdministratorControllerSpec extends ControllerWithQuestionPageBehaviours {

  val formProvider = new RemoveAsSchemeAdministratorFormProvider()
  val form = formProvider()
  val schemeDetails = CommonBuilders.schemeDetailsWithPsaOnlyResponse
  val userAnswer = UserAnswers().schemeName(schemeDetails.schemeDetails.name).srn(schemeDetails.schemeDetails.srn.getOrElse(""))
  val data = userAnswer.dataRetrievalAction
  val validData = userAnswer.removeAsSchemeAdministrator(true).dataRetrievalAction


  val postRequest = FakeRequest().withJsonBody(Json.obj("value" -> true))

  private val fakeSchemeDetailsConnector: SchemeDetailsConnector = new SchemeDetailsConnector {
    override def getSchemeDetails(schemeIdType: String, idNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PsaSchemeDetails] = {
      Future.successful(schemeDetails)
    }
  }

  private def onPageLoadAction(dataRetrievalAction: DataRetrievalAction = data, fakeAuth: AuthAction) = {
    new RemoveAsSchemeAdministratorController(
      frontendAppConfig, fakeAuth, messagesApi, navigator, formProvider,
      FakeUserAnswersCacheConnector, dataRetrievalAction, requiredDataAction, fakeSchemeDetailsConnector).onPageLoad()
  }

  private def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new RemoveAsSchemeAdministratorController(
      frontendAppConfig, fakeAuth, messagesApi, navigator, formProvider,
      FakeUserAnswersCacheConnector, dataRetrievalAction, requiredDataAction, fakeSchemeDetailsConnector).onSubmit()
  }

  def viewAsString(form: Form[Boolean] = form) = removeAsSchemeAdministrator(frontendAppConfig, form, schemeDetails.schemeDetails.name,
    schemeDetails.schemeDetails.srn.getOrElse(""))(fakeRequest, messages).toString

  behave like controllerWithOnPageLoadMethod(onPageLoadAction,
    userAnswer.dataRetrievalAction, validData, form, form.fill(true), viewAsString)

  behave like controllerWithOnSubmitMethod(onSubmitAction, data, form.bind(Map("value" -> "")), viewAsString, postRequest)
}

