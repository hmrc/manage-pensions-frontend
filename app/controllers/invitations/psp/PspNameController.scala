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

package controllers.invitations.psp

import connectors.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.psa.routes._
import forms.invitations.psp.PspNameFormProvider
import identifiers.invitations.psp.PspNameId
import identifiers.{SchemeNameId, SchemeSrnId}
import models.{Mode, SchemeReferenceNumber}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.AuthorisePsp
import utils.{Navigator, UserAnswers}
import views.html.invitations.psp.pspName

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PspNameController @Inject()(
                                   override val messagesApi: MessagesApi,
                                   dataCacheConnector: UserAnswersCacheConnector,
                                   @AuthorisePsp navigator: Navigator,
                                   authenticate: AuthAction,
                                   getData: DataRetrievalAction,
                                   requireData: DataRequiredAction,
                                   formProvider: PspNameFormProvider,
                                   val controllerComponents: MessagesControllerComponents,
                                   view: pspName,
                                   psaSchemeAuthAction: PsaSchemeAuthAction
                                 )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode, srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      SchemeNameId.retrieve.map {
        case schemeName =>
          val value = request.userAnswers.get(PspNameId)
          val preparedForm = value.fold(form)(form.fill)

          Future.successful(Ok(view(preparedForm, mode, schemeName, srn, returnCall(srn))))
      }
  }

  def onSubmit(mode: Mode, srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          SchemeNameId.retrieve.map { case schemeName =>
            Future.successful(BadRequest(view(formWithErrors, mode, schemeName, srn, returnCall(srn))))
          },

        value => {
          dataCacheConnector.save(request.externalId, PspNameId, value).map(
            cacheMap =>
              Redirect(navigator.nextPage(PspNameId, mode, UserAnswers(cacheMap)))
          )
        }
      )
  }

  private def returnCall(srn: String): Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))

}
