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

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{youNeedToRegister, youNeedToRegisterAsPsa, youNeedToRegisterAsPsp}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YouNeedToRegisterController @Inject()(override val authConnector: AuthConnector,
                                            override val messagesApi: MessagesApi,
                                            val controllerComponents: MessagesControllerComponents,
                                            registerAsPspView: youNeedToRegisterAsPsp,
                                            registerAsPsaView: youNeedToRegisterAsPsa,
                                            registerView: youNeedToRegister
                                           )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AuthorisedFunctions {
  def onPageLoad: Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(Retrievals.allEnrolments) {
        enrolments =>
          val isPsa = enrolments.getEnrolment("HMRC-PODS-ORG")
            .flatMap(_.getIdentifier("PSAID")).map(_.value) match {
            case Some(_) => true
            case _ => false
          }

          val isPsp = enrolments.getEnrolment("HMRC-PODSPP-ORG")
            .flatMap(_.getIdentifier("PSPID")).map(_.value) match {
            case Some(_) => true
            case _ => false
          }

          if (isPsa) {
           Future.successful(Ok(registerAsPspView()))
          } else if (isPsp) {
            Future.successful(Ok(registerAsPsaView()))
          } else {
            Future.successful(Ok(registerView()))
          }
      }
  }
}
