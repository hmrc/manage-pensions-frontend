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

import com.google.inject.Inject
import connectors.admin.{DelimitedPractitionerException, MinimalConnector, NoMatchFoundException}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaSchemeAuthAction}
import controllers.psa.routes.PsaSchemeDashboardController
import identifiers.invitations.psp.{PspId, PspNameId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models.SchemeReferenceNumber
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{CheckYourAnswersFactory, PspAuthoriseFuzzyMatcher}
import views.html.invitations.psp.checkYourAnswersPsp

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           checkYourAnswersFactory: CheckYourAnswersFactory,
                                           minimalConnector: MinimalConnector,
                                           pspAuthoriseFuzzyMatcher: PspAuthoriseFuzzyMatcher,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: checkYourAnswersPsp,
                                           psaSchemeAuthAction: PsaSchemeAuthAction
                                          )(implicit val ec: ExecutionContext) extends FrontendBaseController with Retrievals with I18nSupport {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (SchemeNameId and SchemeSrnId).retrieve.map {
        case schemeName ~ srn =>
          val checkYourAnswersHelper = checkYourAnswersFactory.checkYourAnswersHelper(request.userAnswers)
          val sections = Seq(checkYourAnswersHelper.pspName(srn), checkYourAnswersHelper.pspId(srn), checkYourAnswersHelper.pspClientReference(srn)).flatten
          Future.successful(Ok(view(sections, controllers.invitations.psp.routes.CheckYourAnswersController.onSubmit(srn),
            Some("messages__check__your__answer__psp__label"), Some(schemeName), schemeName = schemeName, returnCall = returnCall(srn))))

      }
  }

  def onSubmit(srn: SchemeReferenceNumber): Action[AnyContent] = (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      (PspNameId and PspId).retrieve.map {
        case pspName ~ pspId =>
          minimalConnector.getNameFromPspID(pspId).map {
            case Some(minPspName) if pspAuthoriseFuzzyMatcher.matches(pspName, minPspName) =>
              Redirect(routes.DeclarationController.onPageLoad(srn))
            case _ => Redirect(routes.PspDoesNotMatchController.onPageLoad(srn))
          }.recoverWith {
            case _: DelimitedPractitionerException => Future.successful(Redirect(routes.PspDoesNotMatchController.onPageLoad(srn)))
            case _: NoMatchFoundException => Future.successful(Redirect(routes.PspDoesNotMatchController.onPageLoad(srn)))
          }
      }
  }

  private def returnCall(srn: String): Call = PsaSchemeDashboardController.onPageLoad(SchemeReferenceNumber(srn))
}


