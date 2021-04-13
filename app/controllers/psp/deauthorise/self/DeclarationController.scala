/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.psp.deauthorise.self

import audit.{AuditService, PSPSelfDeauthorisationEmailAuditEvent}
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.admin.MinimalConnector
import connectors.{EmailConnector, EmailNotSent, PspConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.psp.deauthorise.self.routes._
import controllers.routes._
import forms.psp.deauthorise.RemovePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.remove.psp.selfRemoval.RemovalDateId
import identifiers.{AuthorisedPractitionerId, SchemeNameId, SchemeSrnId}
import models.AuthEntity.PSP
import models.requests.DataRequest
import models.{DeAuthorise, MinimalPSAPSP, SendEmailRequest, Sent}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.remove.psp.selfRemoval.declaration

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       formProvider: RemovePspDeclarationFormProvider,
                                       auth: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       pspConnector: PspConnector,
                                       minimalConnector: MinimalConnector,
                                       emailConnector: EmailConnector,
                                       auditService: AuditService,
                                       crypto: ApplicationCrypto,
                                       appConfig: FrontendAppConfig,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: declaration
                                     )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {
  val form: Form[Boolean] = formProvider()

  private val logger = Logger(classOf[DeclarationController])

  def onPageLoad(): Action[AnyContent] = (auth(PSP) andThen getData andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId).retrieve.right.map {
        case srn ~ schemeName =>
          Future.successful(Ok(view(form, schemeName, srn)))
      }
  }

  def onSubmit(): Action[AnyContent] = (auth(PSP) andThen getData andThen requireData).async {
    implicit request =>
      (SchemeSrnId and SchemeNameId and PSTRId and RemovalDateId and AuthorisedPractitionerId).retrieve.right.map {
        case srn ~ schemeName ~ pstr ~ removalDate ~ authorisedPractitioner =>
          form.bindFromRequest().fold(
            (formWithErrors: Form[Boolean]) =>
              Future.successful(BadRequest(view(formWithErrors, schemeName, srn))),
            _ => {
              val pspId = request.pspIdOrException.id
              val deAuthModel: DeAuthorise = DeAuthorise("PSPID", pspId, "PSPID", pspId, removalDate.toString)

              for {
                _ <- pspConnector.deAuthorise(pstr, deAuthModel)
                minimalPSP <- minimalConnector.getMinimalPspDetails(pspId)
                _ <- sendEmail(minimalPSP, authorisedPractitioner.authorisingPSA.name, schemeName, pspId, pstr)
              } yield {
                auditService.sendEvent(PSPSelfDeauthorisationEmailAuditEvent(pspId, pstr, minimalPSP.email, Sent))
                Redirect(ConfirmationController.onPageLoad())
              }
            }
          )
      }
  }

  private def callBackUrl(
                           pspId: String,
                           pstr: String,
                           email: String
                         ): String = {
    val encryptedPspId = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(pspId)).value, StandardCharsets.UTF_8.toString)
    val encryptedPstr = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(pstr)).value, StandardCharsets.UTF_8.toString)
    val encryptedEmail = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(email)).value, StandardCharsets.UTF_8.toString)

    appConfig.urlInThisService(
      EmailResponseController.retrieveStatusForPSPSelfDeauthorisation(encryptedPspId, encryptedPstr, encryptedEmail).url
    )
  }

  private def sendEmail(
                         minimalPSP: MinimalPSAPSP,
                         psaName: String,
                         schemeName: String,
                         pspId: String,
                         pstr: String
                       )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Unit] = {
    val callbackURL = callBackUrl(pspId, pstr, minimalPSP.email)
    val emailTemplateId =
      s"pods_psp_de_auth_psp_${minimalPSP.individualDetails.fold("company_partnership")(_ => "individual")}"
    val sendEmailRequest = SendEmailRequest(
      List(minimalPSP.email),
      emailTemplateId,
      Map(
        "authorisingPsaName" -> psaName,
        "pspName" -> minimalPSP.name,
        "schemeName" -> schemeName
      ),
      force = false,
      eventUrl = Some(callbackURL)
    )

    emailConnector.sendEmail(sendEmailRequest).map { emailStatus =>
      if (emailStatus == EmailNotSent) {
        logger.error("Unable to send email to PSP. Support intervention possibly required.")
      }
      ()
    }

  }
}
