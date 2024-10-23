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

package controllers

import controllers.actions.NoBothEnrolmentsOnlyAuthAction
import models.FeatureToggleName.EnrolmentRecovery
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.FeatureToggleService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{youNeedToRegister, youNeedToRegisterAsPsa, youNeedToRegisterAsPsp, youNeedToRegisterOld}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YouNeedToRegisterController @Inject()(override val messagesApi: MessagesApi,
                                            val controllerComponents: MessagesControllerComponents,
                                            toggleService: FeatureToggleService,
                                            registerAsPspView: youNeedToRegisterAsPsp,
                                            registerAsPsaView: youNeedToRegisterAsPsa,
                                            registerView: youNeedToRegister,
                                            viewOld: youNeedToRegisterOld,
                                            noBothEnrolmentsOnlyAuthAction: NoBothEnrolmentsOnlyAuthAction
                                           )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  def onPageLoad: Action[AnyContent] = noBothEnrolmentsOnlyAuthAction.async {
    implicit request =>
      val isPsa = request.psaId.isDefined

      val isPsp = request.pspId.isDefined

      if (isPsa) {
        Future.successful(Ok(registerAsPspView()))
      } else if (isPsp) {
        Future.successful(Ok(registerAsPsaView()))
      } else {
        toggleService.get(EnrolmentRecovery).map { toggleValue =>
          if (toggleValue.isEnabled) {
            Ok(registerView())
          } else {
            Ok(viewOld())
          }
        }
      }
  }
}
