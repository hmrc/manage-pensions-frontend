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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.InvitationConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.SchemeDetailId
import identifiers.invitations.{CheckYourAnswersId, PSAId, PsaNameId}
import models.NormalMode
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{CheckYourAnswersFactory, DateHelper, Navigator}
import utils.annotations.Invitation
import viewmodels.AnswerSection
import views.html.check_your_answers

import scala.concurrent.Future

class CheckYourAnswersController @Inject()(appConfig: FrontendAppConfig,
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           @Invitation navigator: Navigator,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           checkYourAnswersFactory: CheckYourAnswersFactory,
                                           invitationConnector: InvitationConnector) extends FrontendController  with Retrievals with I18nSupport {

  def onPageLoad() = (authenticate andThen getData andThen requireData).async {
    implicit request =>

      SchemeDetailId.retrieve.right.map {schemeDetail =>

        val checkYourAnswersHelper = checkYourAnswersFactory.checkYourAnswersHelper(request.userAnswers)
        val sections = Seq(AnswerSection(None, Seq(checkYourAnswersHelper.psaName, checkYourAnswersHelper.psaId).flatten))

        Future.successful(Ok(check_your_answers(appConfig, sections, None, controllers.invitations.routes.CheckYourAnswersController.onSubmit(),
          Some("messages__check__your__answer__main__containt__label"), Some(schemeDetail.schemeName))))

      }
  }

  def onSubmit() = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (SchemeDetailId and PsaNameId and PSAId).retrieve.right.map {
        case (schemeDetails ~ inviteeName ~ inviterPsaId) if (schemeDetails.pstr.isDefined) =>
        val invitation = models.Invitation(schemeDetails.srn,
          schemeDetails.pstr.get,
          schemeDetails.schemeName,
          request.psaId.id,
          inviterPsaId,
          inviteeName,
          getExpireAt
        )

        invitationConnector.invite(invitation).map{
          case CREATED => Redirect(controllers.invitations.routes.InvitationSuccessController.onPageLoad)
          case _ => Redirect(controllers.routes.SessionExpiredController.onPageLoad())
        }
        case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  private def getExpireAt = DateHelper.dateTimeFromNowToMidnightAfterDays(appConfig.invitationExpiryDays)


}
