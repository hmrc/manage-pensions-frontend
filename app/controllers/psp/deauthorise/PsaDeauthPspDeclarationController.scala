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

package controllers.psp.deauthorise

import audit.{AuditService, PSPDeauthorisationByPSAAuditEvent, PSPDeauthorisationByPSAEmailAuditEvent}
import config.FrontendAppConfig
import connectors.admin.MinimalConnector
import connectors.{EmailConnector, EmailNotSent, PspConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaSchemeAuthAction}
import controllers.routes._
import forms.psp.deauthorise.DeauthorisePspDeclarationFormProvider
import identifiers.SchemeNameId
import identifiers.invitations.PSTRId
import identifiers.psp.deauthorise
import identifiers.psp.deauthorise.{PsaDeauthorisePspDeclarationId, PspDeauthDateId, PspDetailsId}
import models._
import models.requests.DataRequest
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.annotations.DeauthorisePSP
import utils.{Navigator, UserAnswers}
import views.html.psp.deauthorisation.psaDeauthorisePspDeclaration

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsaDeauthPspDeclarationController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   userAnswersCacheConnector: UserAnswersCacheConnector,
                                                   @DeauthorisePSP navigator: Navigator,
                                                   authenticate: AuthAction,
                                                   getData: DataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   pspConnector: PspConnector,
                                                   formProvider: DeauthorisePspDeclarationFormProvider,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   auditService: AuditService,
                                                   minimalConnector: MinimalConnector,
                                                   appConfig: FrontendAppConfig,
                                                   emailConnector: EmailConnector,
                                                   crypto: ApplicationCrypto,
                                                   view: psaDeauthorisePspDeclaration,
                                                   psaSchemeAuthAction: PsaSchemeAuthAction
                                                 )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  private val logger = Logger(classOf[PsaDeauthPspDeclarationController])

  private def form: Form[Boolean] = formProvider()

  def onPageLoad(index: Index, srn: SchemeReferenceNumber): Action[AnyContent] =
    (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
      implicit request =>
        (SchemeNameId and deauthorise.PspDetailsId(index)).retrieve.map {
          case schemeName ~ pspDetails =>
            if (pspDetails.authorisingPSAID == request.psaIdOrException.id) {
              Future.successful(Ok(view(form, schemeName, srn, index)))
            } else {
              Future.successful(Redirect(SessionExpiredController.onPageLoad))
            }
        }
    }

  def onSubmit(index: Index, srn: SchemeReferenceNumber): Action[AnyContent] =
    (authenticate() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
      implicit request =>
        (SchemeNameId and PspDetailsId(index) and PSTRId and PspDeauthDateId(index)).retrieve.map {
          case schemeName ~ pspDetails ~ pstr ~ removalDate =>
            if (pspDetails.authorisingPSAID == request.psaIdOrException.id) {
              form.bindFromRequest().fold(
                (formWithErrors: Form[Boolean]) =>
                  Future.successful(BadRequest(view(formWithErrors, schemeName, srn, index))),
                value => {
                  val psaId = request.psaIdOrException.id
                  for {
                    cacheMap <- userAnswersCacheConnector.save(request.externalId, PsaDeauthorisePspDeclarationId(index), value)
                    _ <- pspConnector.deAuthorise(
                      pstr = pstr,
                      deAuthorise = DeAuthorise(
                        ceaseIDType = "PSPID",
                        ceaseNumber = pspDetails.id,
                        initiatedIDType = "PSAID",
                        initiatedIDNumber = request.psaIdOrException.id,
                        ceaseDate = removalDate.toString
                      )
                    )
                    minimalPSAPSP <- minimalConnector.getMinimalPsaDetails(psaId)
                    _ <- sendEmail(minimalPSAPSP, psaId, pspDetails.id, pstr, pspDetails.name, schemeName)
                  } yield {
                    auditService.sendEvent(
                      PSPDeauthorisationByPSAEmailAuditEvent(
                        psaId = psaId,
                        pspId = pspDetails.id,
                        pstr = pstr,
                        minimalPSAPSP.email
                      )
                    )
                    auditService.sendExtendedEvent(
                      PSPDeauthorisationByPSAAuditEvent(removalDate, psaId, pspDetails.id, pstr)
                    )
                    Redirect(navigator.nextPage(deauthorise.PsaDeauthorisePspDeclarationId(index), NormalMode, UserAnswers(cacheMap)))
                  }
                }
              )
            } else {
              Future.successful(Redirect(SessionExpiredController.onPageLoad))
            }
        }
    }

  private def callBackUrl(
                           psaId: String,
                           pspId: String,
                           pstr: String,
                           email: String
                         ): String = {
    val encryptedPsaId = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(psaId)).value, StandardCharsets.UTF_8.toString)
    val encryptedPspId = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(pspId)).value, StandardCharsets.UTF_8.toString)
    val encryptedPstr = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(pstr)).value, StandardCharsets.UTF_8.toString)
    val encryptedEmail = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(email)).value, StandardCharsets.UTF_8.toString)
    appConfig.urlInThisService(
      EmailResponseController.retrieveStatusForPSPDeauthorisation(encryptedPsaId, encryptedPspId, encryptedPstr, encryptedEmail)
        .url
    )
  }

  private def sendEmail(
                         minimalPSAPSP: MinimalPSAPSP,
                         psaId: String,
                         pspId: String,
                         pstr: String,
                         pspName: String,
                         schemeName: String
                       )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Unit] = {
    val callbackURL = callBackUrl(psaId, pspId, pstr, minimalPSAPSP.email)
    emailConnector.sendEmail(
      SendEmailRequest(
        to = List(minimalPSAPSP.email),
        templateId = appConfig.emailPsaDeauthorisePspTemplateId,
        parameters = Map(
          "psaName" -> minimalPSAPSP.name,
          "pspName" -> pspName,
          "schemeName" -> schemeName
        ),
        force = false,
        eventUrl = Some(callbackURL)
      )
    ).map { emailStatus =>
      if (emailStatus == EmailNotSent) {
        logger.error("Unable to send email to de-authorising PSA. Support intervention possibly required.")
      }
      ()
    }
  }
}
