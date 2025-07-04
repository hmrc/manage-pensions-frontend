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

package controllers.psp.view

import com.google.inject.Inject
import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.psa.routes._
import controllers.psp.view.routes._
import forms.invitations.psp.PspHasClientReferenceFormProvider
import identifiers.psp.deauthorise.PspDetailsId
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{Mode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.invitations.psp.pspHasClientReference

import scala.concurrent.{ExecutionContext, Future}

class ViewPspHasClientReferenceController @Inject()(
                                                     override val messagesApi: MessagesApi,
                                                     authenticate: AuthAction,
                                                     getData: DataRetrievalAction,
                                                     requireData: DataRequiredAction,
                                                     formProvider: PspHasClientReferenceFormProvider,
                                                     dataCacheConnector: UserAnswersCacheConnector,
                                                     val controllerComponents: MessagesControllerComponents,
                                                     view: pspHasClientReference,
                                                     psaSchemeAuthAction: PsaSchemeAuthAction
                                                   )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode, index: Int, srn: SchemeReferenceNumber): Action[AnyContent] =
                (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId and PspDetailsId(index)).retrieve.map {
        case srn ~ schemeName ~ pspDetail =>
          if (pspDetail.authorisingPSAID == request.psaIdOrException.id) {
            val value = pspDetail.clientReference
            val hasClientRef = value match {
              case Some(_) => Some(true)
              case _ => Some(false)
            }
            val preparedForm = hasClientRef.fold(form)(form.fill)
            Future.successful(Ok(view(preparedForm, pspDetail.name, mode, schemeName, returnCall(srn),
              ViewPspHasClientReferenceController.onSubmit(mode, index, srn))))
          } else {
            Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
          }
      }
  }

  def onSubmit(mode: Mode, index: Int, srn: SchemeReferenceNumber): Action[AnyContent] =
               (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[?]) => {
          (SchemeSrnId and SchemeNameId and PspDetailsId(index)).retrieve.map {
            case srn ~ schemeName ~ pspDetail =>
              Future.successful(BadRequest(view(formWithErrors, pspDetail.name, mode, schemeName, returnCall(srn),
                ViewPspHasClientReferenceController.onSubmit(mode, index, srn))))
          }
        },
        value =>
          if (value) {
            Future.successful(Redirect(controllers.psp.view.routes.ViewPspClientReferenceController.onPageLoad(mode, index, srn)))
          } else {
            PspDetailsId(index).retrieve.map {
              pspDetail =>
                if (pspDetail.authorisingPSAID == request.psaIdOrException.id) {
                  val updatedPspDetail = pspDetail.copy(clientReference = None)
                  dataCacheConnector.save(request.externalId, PspDetailsId(index), updatedPspDetail).map(_ =>
                    Redirect(controllers.psp.view.routes.ViewPspCheckYourAnswersController.onPageLoad(index, srn))
                  )
                } else {
                  Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
                }
            }
          }
      )
  }

  private def returnCall(srn: String): Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))

}
