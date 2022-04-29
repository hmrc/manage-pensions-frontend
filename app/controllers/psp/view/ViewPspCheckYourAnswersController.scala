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

package controllers.psp.view

import com.google.inject.Inject
import connectors.UpdateClientReferenceConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.psa.routes.PsaSchemeDashboardController
import identifiers.invitations.PSTRId
import identifiers.psp.deauthorise.PspDetailsId
import identifiers.{SchemeNameId, SchemeSrnId}
import models.SchemeReferenceNumber
import models.psp.UpdateClientReferenceRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ViewPspCheckYourAnswersHelper
import views.html.invitations.psp.checkYourAnswersPsp

import scala.concurrent.{ExecutionContext, Future}

class ViewPspCheckYourAnswersController @Inject()(override val messagesApi: MessagesApi,
                                                  authenticate: AuthAction,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  updateClientReferenceConnector: UpdateClientReferenceConnector,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: checkYourAnswersPsp
                                                 )(implicit val ec: ExecutionContext) extends FrontendBaseController with Retrievals with I18nSupport {

  def onPageLoad(index: Int): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId and PspDetailsId(index)).retrieve.right.map {
        case srn ~ schemeName ~ pspDetail =>
          if (pspDetail.authorisingPSAID == request.psaIdOrException.id) {
            val helper: ViewPspCheckYourAnswersHelper = new ViewPspCheckYourAnswersHelper()
            val sections = Seq(helper.pspName(pspDetail.name), helper.pspId(pspDetail.id),
              helper.pspClientReference(pspDetail.clientReference, index))

            Future.successful(Ok(view(sections, controllers.psp.view.routes.ViewPspCheckYourAnswersController.onSubmit(index),
              schemeName = schemeName, returnCall = returnCall(srn))))
          } else {
            Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }
      }
  }

  def onSubmit(index: Int): Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      (PSTRId and PspDetailsId(index)).retrieve.right.map {
        case pstr ~ pspDetail =>
          if (pspDetail.authorisingPSAID == request.psaIdOrException.id) {
            val psaId = request.psaIdOrException.id
            val updateClientReferenceRequest: UpdateClientReferenceRequest = UpdateClientReferenceRequest(pstr, psaId, pspDetail.id, pspDetail.clientReference)

            updateClientReferenceConnector.updateClientReference(updateClientReferenceRequest).map(_ =>
              Redirect(controllers.psp.routes.ViewPractitionersController.onPageLoad())
            )
          } else {
            Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
          }
      }
  }

  private def returnCall(srn: String): Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))
}

