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

package controllers.remove

import java.time.LocalDate

import config.FrontendAppConfig
import connectors.EmailConnector
import connectors.{UserAnswersCacheConnector, PspConnector}
import controllers.Retrievals
import controllers.actions.{DataRequiredAction, AuthAction, DataRetrievalAction}
import forms.remove.PsaRemovePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.{SchemeNameId, SchemeSrnId}
import identifiers.remove.{PsaRemovePspDeclarationId, PspDetailsId}
import javax.inject.Inject
import models.SendEmailRequest
import models.invitations.psp.DeAuthorise
import models.requests.AuthenticatedRequest
import models.requests.DataRequest
import models.requests.OptionalDataRequest
import models.{NormalMode, Index}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Action}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.PensionAdminCache
import utils.{Navigator, UserAnswers}
import utils.annotations.RemovePSP
import views.html.remove.psaRemovePspDeclaration

import scala.concurrent.{Future, ExecutionContext}

class PsaRemovePspDeclarationController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   userAnswersCacheConnector: UserAnswersCacheConnector,
                                                   @RemovePSP navigator: Navigator,
                                                   authenticate: AuthAction,
                                                   getData: DataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   pspConnector: PspConnector,
                                                   formProvider: PsaRemovePspDeclarationFormProvider,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   @PensionAdminCache userAnswersCacheConnectorPsaAdmin: UserAnswersCacheConnector,
                                                   appConfig: FrontendAppConfig,
                                                   emailConnector: EmailConnector,
                                                   view: psaRemovePspDeclaration
                                                 )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  private def form: Form[Boolean] = formProvider()

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate() andThen getData andThen requireData).async {
      implicit request =>
        (SchemeSrnId and SchemeNameId and PspDetailsId(index)).retrieve.right.map {
          case srn ~ schemeName ~ pspDetails =>
            if (pspDetails.authorisingPSAID == request.psaIdOrException.id) {
              Future.successful(Ok(view(form, schemeName, srn, index)))
            } else {
              Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
            }
        }
    }

  def onSubmit(index: Index): Action[AnyContent] =
    (authenticate() andThen getData andThen requireData).async {
      implicit request =>
        (SchemeSrnId and SchemeNameId and PspDetailsId(index) and PSTRId).retrieve.right.map {
          case srn ~ schemeName ~ pspDetails ~ pstr =>
            if (pspDetails.authorisingPSAID == request.psaIdOrException.id) {
              form.bindFromRequest().fold(
                (formWithErrors: Form[Boolean]) =>
                  Future.successful(BadRequest(view(formWithErrors, schemeName, srn, index))),
                value =>
                  for {
                    cacheMap <- userAnswersCacheConnector.save(request.externalId, PsaRemovePspDeclarationId(index), value)
                    _ <-  pspConnector.deAuthorise(
                        pstr = pstr,
                        deAuthorise = DeAuthorise(
                          ceaseIDType = "PSP",
                          ceaseNumber = pspDetails.id,
                          initiatedIDType = "PSA",
                          initiatedIDNumber = request.psaIdOrException.id,
                          ceaseDate = LocalDate.now().toString
                      )
                    )
                    emailAddress <- getEmailAddress(request)
                    _ <- emailConnector.sendEmail(
                        SendEmailRequest(
                          to = List(emailAddress),
                          templateId = appConfig.emailPsaDeauthorisePspTemplateId,
                          parameters = Map("" -> "")
                        )
                      )
                  } yield {
                    Redirect(navigator.nextPage(PsaRemovePspDeclarationId(index), NormalMode, UserAnswers(cacheMap)))
                  }
              )
            } else {
              Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
            }
        }
    }

  private def getEmailAddress(request: DataRequest[_])(implicit ec: ExecutionContext): Future[String] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    val futureOptionPsaUA = userAnswersCacheConnectorPsaAdmin.fetch(request.externalId).map {
      case None => None
      case Some(data) => Some(UserAnswers(data))
    }

    val futureOptionEmailAddress = futureOptionPsaUA.map { optionUA =>
      optionUA.flatMap { ua =>
        val individual = (ua.json \ "individualContactDetails" \ "email").asOpt[String]
        val company = (ua.json \ "contactDetails" \ "email").asOpt[String]
        val partnership = (ua.json \ "partnershipContactDetails" \ "email").asOpt[String]

        (individual, company, partnership) match {
          case (e@Some(_), _, _) => e
          case (_, e@Some(_), _) => e
          case (_, _, e@Some(_)) => e
          case _ => None
        }
      }
    }

    futureOptionEmailAddress.map {
      case None => throw new RuntimeException("No email address found for PSA")
      case Some(e) => e
    }
  }
}
