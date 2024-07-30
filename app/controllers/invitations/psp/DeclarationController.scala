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

import audit.{AuditService, PSPAuthorisationAuditEvent, PSPAuthorisationEmailAuditEvent}
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.admin.MinimalConnector
import connectors.scheme.ListOfSchemesConnector
import connectors.{ActiveRelationshipExistsException, EmailConnector, EmailNotSent, PspConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaPspSchemeAuthAction}
import forms.invitations.psp.DeclarationFormProvider
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models.requests.DataRequest
import models.{MinimalPSAPSP, SchemeReferenceNumber, SendEmailRequest, Sent}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDetailsService
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.invitations.psp.declaration

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       formProvider: DeclarationFormProvider,
                                       auth: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       pspConnector: PspConnector,
                                       listOfSchemesConnector: ListOfSchemesConnector,
                                       schemeDetailsService: SchemeDetailsService,
                                       emailConnector: EmailConnector,
                                       minimalConnector: MinimalConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       auditService: AuditService,
                                       crypto: ApplicationCrypto,
                                       appConfig: FrontendAppConfig,
                                       view: declaration,
                                       psaSchemeAuthAction: PsaPspSchemeAuthAction
                                     )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  private val logger = Logger(classOf[DeclarationController])

  val form: Form[Boolean] = formProvider()

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = (auth() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData) {
    implicit request =>
      Ok(view(form))
  }

  def onSubmit(srn: SchemeReferenceNumber): Action[AnyContent] = (auth() andThen getData andThen psaSchemeAuthAction(srn) andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        _ => inviteEmailAndRedirect()
      )
  }

  private def inviteEmailAndRedirect()(implicit request: DataRequest[AnyContent]): Future[Result] =
    (SchemeNameId and SchemeSrnId and PspNameId and PspId).retrieve.map {
      case schemeName ~ srn ~ pspName ~ pspId =>
        getPstr(srn).flatMap {
          case Some(pstr) =>
            val psaId = request.psaIdOrException.id
            val pspCR = request.userAnswers.get(PspClientReferenceId)
            pspConnector.authorisePsp(pstr, psaId, pspId, pspCR).flatMap { _ =>
              minimalConnector.getMinimalPsaDetails(psaId).flatMap { minimalPSAPSP =>
                sendEmail(minimalPSAPSP, psaId, pspId, pstr, pspName, schemeName).map { _ =>
                  auditService.sendEvent(
                    PSPAuthorisationEmailAuditEvent(
                      psaId = request.psaIdOrException.id,
                      pspId = pspId,
                      pstr = pstr,
                      minimalPSAPSP.email,
                      Sent
                    )
                  )
                  auditService.sendEvent(
                    PSPAuthorisationAuditEvent(
                      psaId = request.psaIdOrException.id,
                      pspId = pspId,
                      pstr = pstr
                    )
                  )
                  Redirect(routes.ConfirmationController.onPageLoad(srn))
                }
              }
            } recoverWith {
              case _: ActiveRelationshipExistsException =>
                Future.successful(Redirect(controllers.invitations.psp.routes.AlreadyAssociatedWithSchemeController.onPageLoad(srn)))
            }
          case _ =>
            Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
        }
    }.left.map(_ =>
      Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
    )


  private def getPstr(srn: String)(implicit request: DataRequest[AnyContent]): Future[Option[String]] =
    listOfSchemesConnector.getListOfSchemes(request.psaIdOrException.id).map {
      case Right(list) => schemeDetailsService.pstr(srn, list)
      case _ => None
    }

  private def callBackUrl(
                           psaId: String,
                           pspId: String,
                           pstr: String,
                           email: String
                         ): String =
    appConfig.urlInThisService(
      controllers.routes.EmailResponseController.retrieveStatusForPSPAuthorisation(
        encodeAndEncrypt(psaId), encodeAndEncrypt(pspId), encodeAndEncrypt(pstr), encodeAndEncrypt(email)
      ).url
    )

  private def encodeAndEncrypt(s: String): String =
    URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(s)).value, StandardCharsets.UTF_8.toString)


  private def sendEmail(
                         minimalPSAPSP: MinimalPSAPSP,
                         psaId: String,
                         pspId: String,
                         pstr: String,
                         pspName: String,
                         schemeName: String
                       )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Unit] = {
    val callbackURL = callBackUrl(psaId, pspId, pstr, minimalPSAPSP.email)
    val email = SendEmailRequest(
      List(minimalPSAPSP.email),
      "pods_authorise_psp",
      Map(
        "psaInvitor" -> minimalPSAPSP.name,
        "pspInvitee" -> pspName,
        "schemeName" -> schemeName
      ),
      force = false,
      eventUrl = Some(callbackURL)
    )

    emailConnector.sendEmail(email).map { emailStatus =>
      if (emailStatus == EmailNotSent) {
        logger.error("Unable to send email to authorising PSA. Support intervention possibly required.")
      }
      ()
    }

  }
}
