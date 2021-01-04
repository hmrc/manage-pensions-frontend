/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.Retrievals
import controllers.actions.AuthAction
import controllers.actions.DataRequiredAction
import controllers.actions.DataRetrievalAction
import identifiers.invitations.AdviserNameId
import identifiers.invitations.CheckPensionAdviserAnswersId
import models.NormalMode
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.AcceptInvitation
import utils.CheckYourAnswersFactory
import utils.Navigator
import viewmodels.AnswerSection
import viewmodels.Message
import views.html.check_your_answers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CheckPensionAdviserAnswersController @Inject()(appConfig: FrontendAppConfig,
                                                     override val messagesApi: MessagesApi,
                                                     authenticate: AuthAction,
                                                     @AcceptInvitation navigator: Navigator,
                                                     getData: DataRetrievalAction,
                                                     requireData: DataRequiredAction,
                                                     checkYourAnswersFactory: CheckYourAnswersFactory,
                                                     val controllerComponents: MessagesControllerComponents,
                                                     view: check_your_answers
                                                    )(implicit val ec: ExecutionContext) extends FrontendBaseController with Retrievals with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      val checkYourAnswersHelper = checkYourAnswersFactory.checkYourAnswersHelper(request.userAnswers)
      AdviserNameId.retrieve.right.map { name =>

        val dynamicEmailLabel = Message("messages__check__your__answer__adviser__email__label", name)
        val dynamicAddressLabel = Message("messages__check__your__answer__adviser__address__label", name)

        val sections = Seq(AnswerSection(None, Seq(
          checkYourAnswersHelper.adviserName,
          checkYourAnswersHelper.adviserEmail(dynamicEmailLabel),
          checkYourAnswersHelper.adviserAddress(dynamicAddressLabel
          )).flatten))

        Future.successful(Ok(view(sections, None, controllers.invitations.routes.CheckPensionAdviserAnswersController.onSubmit())))
      }
  }

  def onSubmit(): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      Future.successful(Redirect(navigator.nextPage(CheckPensionAdviserAnswersId, NormalMode, request.userAnswers)))
  }
}
