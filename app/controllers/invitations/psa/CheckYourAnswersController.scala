/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.scheme.SchemeDetailsConnector
import connectors.{InvitationConnector, NameMatchingFailedException, PsaAlreadyInvitedException}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.invitations.psa.routes._
import identifiers.MinimalSchemeDetailId
import identifiers.invitations.psa.InviteePSAId
import identifiers.invitations.{CheckYourAnswersId, InviteeNameId}
import models.psa.PsaDetails
import models.invitations.Invitation
import models.requests.DataRequest
import models.{MinimalSchemeDetail, NormalMode, SchemeReferenceNumber}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.Invitations
import utils.{CheckYourAnswersFactory, DateHelper, Navigator}
import views.html.check_your_answers

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(appConfig: FrontendAppConfig,
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           @Invitations navigator: Navigator,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           checkYourAnswersFactory: CheckYourAnswersFactory,
                                           schemeDetailsConnector: SchemeDetailsConnector,
                                           invitationConnector: InvitationConnector,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: check_your_answers
                                          )(implicit val ec: ExecutionContext) extends FrontendBaseController with Retrievals with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>

      MinimalSchemeDetailId.retrieve.right.map { schemeDetail =>

        val checkYourAnswersHelper = checkYourAnswersFactory.checkYourAnswersHelper(request.userAnswers)
        val sections = Seq(checkYourAnswersHelper.psaName, checkYourAnswersHelper.psaId).flatten

        Future.successful(Ok(view(sections, None, CheckYourAnswersController.onSubmit(),
          Some("messages__check__your__answer__main__containt__label"), Some(schemeDetail.schemeName))))

      }
  }

  private def isSchemeAssociatedWithInvitee(psaId: String, srn: String,
                                            inviteePsaId: String)
                                           (implicit request: Request[_]): Future[Boolean] =
    schemeDetailsConnector.getSchemeDetails(
      psaId = psaId,
      idNumber = srn,
      schemeIdType = "srn"
    ).map { scheme =>
      (scheme.json \ "psaDetails").toOption.exists(_.as[Seq[PsaDetails]].exists(_.id == inviteePsaId))
    }


  def onSubmit(): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      (MinimalSchemeDetailId and InviteeNameId and InviteePSAId).retrieve.right.map {
        case schemeDetails ~ inviteeName ~ inviteePsaId if schemeDetails.pstr.isDefined =>
          val invitation = Invitation(
            SchemeReferenceNumber(schemeDetails.srn),
            schemeDetails.pstr.get,
            schemeDetails.schemeName,
            request.psaIdOrException,
            PsaId(inviteePsaId),
            inviteeName,
            getExpireAt
          )

          isSchemeAssociatedWithInvitee(request.psaIdOrException.id, schemeDetails.srn, inviteePsaId).flatMap { isAssociated =>
            invite(invitation, schemeDetails).map(result =>
              if (isAssociated) Redirect(routes.PsaAlreadyAssociatedController.onPageLoad()) else result
            )
          }.recoverWith {
            case _: NameMatchingFailedException =>
              Future.successful(Redirect(IncorrectPsaDetailsController.onPageLoad()))
            case _ =>
              Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }
      }
  }

  private def invite(invite: Invitation, msd: MinimalSchemeDetail)
                    (implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Result] = {
    invitationConnector.invite(invite)
      .map(_ => Redirect(navigator.nextPage(CheckYourAnswersId(msd.srn), NormalMode, request.userAnswers)))
      .recoverWith {
        case _: NotFoundException =>
          Future.successful(Redirect(IncorrectPsaDetailsController.onPageLoad()))
        case _: PsaAlreadyInvitedException =>
          Future.successful(Redirect(InvitationDuplicateController.onPageLoad()))
      }
  }

  private def getExpireAt: LocalDateTime = DateHelper.dateTimeFromNowToMidnightAfterDays(appConfig.invitationExpiryDays)

}
