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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.admin.MinimalPsaConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.invitations.psp.{PspId, PspNameId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models.SchemeReferenceNumber
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.Invitation
import utils.{CheckYourAnswersFactory, Navigator}
import viewmodels.AnswerSection
import views.html.check_your_answers

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(appConfig: FrontendAppConfig,
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           checkYourAnswersFactory: CheckYourAnswersFactory,
                                           minimalConnector: MinimalPsaConnector,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: check_your_answers
                                          )(implicit val ec: ExecutionContext) extends FrontendBaseController with Retrievals with I18nSupport {

    def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
        implicit request =>

            SchemeNameId.retrieve.right.map { schemeName =>
                val checkYourAnswersHelper = checkYourAnswersFactory.checkYourAnswersHelper(request.userAnswers)
                val sections = Seq(AnswerSection(None, Seq(checkYourAnswersHelper.pspName, checkYourAnswersHelper.pspId,
                    checkYourAnswersHelper.pspClientReference).flatten))
                Future.successful(Ok(view(sections, None, controllers.invitations.psp.routes.CheckYourAnswersController.onSubmit(),
                    Some("messages__check__your__answer__psp__label"), Some(schemeName), Some("site.save_and_continue"))))

            }
    }

    def onSubmit(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
        implicit request =>
            (PspNameId and PspId).retrieve.right.map {
                case pspName ~ pspId =>
                    minimalConnector.getNameFromPspID(pspId).map {
                        case Some(minPspName) if pspName.equalsIgnoreCase(minPspName) =>
                            Redirect(controllers.invitations.psp.routes.DeclarationController.onPageLoad())
                        case _ => //todo interrupt page
                            Redirect(controllers.routes.SessionExpiredController.onPageLoad())
                    }
            }
    }
}

