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
import connectors.{InvitationConnector, NameMatchingFailedException, PsaAlreadyInvitedException, SchemeDetailsConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.invitations.{CheckYourAnswersId, InviteeNameId, InviteePSAId}
import identifiers.{MinimalSchemeDetailId, SchemeSrnId}
import models.{NormalMode, PsaDetails, SchemeReferenceNumber}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.Invitation
import utils.{CheckYourAnswersFactory, DateHelper, Navigator}
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
                                           schemeDetailsConnector: SchemeDetailsConnector,
                                           invitationConnector: InvitationConnector) extends FrontendController with Retrievals with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>

      MinimalSchemeDetailId.retrieve.right.map { schemeDetail =>

        val checkYourAnswersHelper = checkYourAnswersFactory.checkYourAnswersHelper(request.userAnswers)
        val sections = Seq(AnswerSection(None, Seq(checkYourAnswersHelper.psaName, checkYourAnswersHelper.psaId).flatten))

        Future.successful(Ok(check_your_answers(appConfig, sections, None, controllers.invitations.routes.CheckYourAnswersController.onSubmit(),
          Some("messages__check__your__answer__main__containt__label"), Some(schemeDetail.schemeName))))

      }
  }

  private def isSchemeAssociatedWithInvitee(srn: String, inviteePsaId: String)(implicit request: Request[_]): Future[Boolean] =
    schemeDetailsConnector.getSchemeDetails("srn", srn)
      .map(_.psaDetails.fold[Seq[PsaDetails]](Seq.empty)(identity).exists(_.id == inviteePsaId))

  def onSubmit(): Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (MinimalSchemeDetailId and InviteeNameId and InviteePSAId).retrieve.right.map {
        case schemeDetails ~ inviteeName ~ inviteePsaId if schemeDetails.pstr.isDefined =>
          val invitation = models.Invitation(
            SchemeReferenceNumber(schemeDetails.srn),
            schemeDetails.pstr.get,
            schemeDetails.schemeName,
            request.psaId,
            PsaId(inviteePsaId),
            inviteeName,
            getExpireAt
          )
          isSchemeAssociatedWithInvitee(schemeDetails.srn, inviteePsaId).flatMap {
              if (_) {
                Future.successful(Redirect(routes.PsaAlreadyAssociatedController.onPageLoad()))
              } else {
                invitationConnector.invite(invitation)
                  .map(_ => Redirect(navigator.nextPage(CheckYourAnswersId(schemeDetails.srn), NormalMode, request.userAnswers)))
              }
            }.recoverWith {
              case _: NameMatchingFailedException =>
                Future.successful(Redirect(controllers.invitations.routes.IncorrectPsaDetailsController.onPageLoad()))
              case _: PsaAlreadyInvitedException =>
                Future.successful(Redirect(controllers.invitations.routes.InvitationDuplicateController.onPageLoad()))
              case _ =>
                Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
            }
        case _ =>
          Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  private def getExpireAt = DateHelper.dateTimeFromNowToMidnightAfterDays(appConfig.invitationExpiryDays)

}
