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

package controllers.invitations.psp

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import audit.{AuditService, PSPAuthorisationEmailAuditEvent}
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.{ActiveRelationshipExistsException, EmailConnector, EmailNotSent, PspConnector}
import connectors.admin.MinimalConnector
import connectors.scheme.ListOfSchemesConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.invitations.psp.DeclarationFormProvider
import identifiers.{SchemeNameId, SchemeSrnId}
import identifiers.invitations.psp.{PspClientReferenceId, PspId, PspNameId}
import models.{MinimalPSAPSP, SendEmailRequest}
import models.invitations.psp.ClientReference
import models.requests.DataRequest
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDetailsService
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.invitations.psp.declaration

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
                                       view: declaration
                                     )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {
  val form: Form[Boolean] = formProvider()
  val sessionExpired: Future[Result] =
    Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))

  def onPageLoad(): Action[AnyContent] = (auth() andThen getData andThen requireData) {
    implicit request =>
      Ok(view(form))
  }

  def onSubmit(): Action[AnyContent] = (auth() andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        _ => inviteEmailAndRedirect()
      )
  }

  private def inviteEmailAndRedirect()(implicit request: DataRequest[AnyContent]): Future[Result] =
    (SchemeNameId and SchemeSrnId and PspNameId and PspId and PspClientReferenceId).retrieve.right.map {
      case schemeName ~ srn ~ pspName ~ pspId ~ pspCR =>
        getPstr(srn).flatMap {
          case Some(pstr) =>
            val psaId = request.psaIdOrException.id
            pspConnector.authorisePsp(pstr, psaId, pspId, getClientReference(pspCR)).flatMap { _ =>
              minimalConnector.getMinimalPsaDetails(psaId).flatMap { minimalPSAPSP =>
                sendEmail(minimalPSAPSP, psaId, pspId, pstr, pspName, schemeName).map { _ =>
                  auditService.sendEvent(
                    PSPAuthorisationEmailAuditEvent(
                      psaId = request.psaIdOrException.id,
                      pspId = pspId,
                      pstr = pstr,
                      minimalPSAPSP.email
                    )
                  )
                  Redirect(routes.ConfirmationController.onPageLoad())
                }
              }
            } recoverWith {
              case _: ActiveRelationshipExistsException =>
                Future.successful(Redirect(controllers.invitations.psp.routes.AlreadyAssociatedWithSchemeController.onPageLoad()))
            }
          case _ => sessionExpired
        }
    }.left.map(_ => sessionExpired)


  private def getPstr(srn: String)(implicit request: DataRequest[AnyContent]): Future[Option[String]] =
    listOfSchemesConnector.getListOfSchemes(request.psaIdOrException.id).map {
      case Right(list) => schemeDetailsService.pstr(srn, list)
      case _ => None
    }

  private def getClientReference(answer: ClientReference): Option[String] = answer match {
    case ClientReference.HaveClientReference(reference) => Some(reference)
    case ClientReference.NoClientReference => None
  }

  private def callBackUrl(
                           psaId: String,
                           pspId: String,
                           pstr: String,
                           email: String
                         ): String =
    appConfig.pspAuthEmailCallback(
      encodeAndEncrypt(psaId), encodeAndEncrypt(pspId), encodeAndEncrypt(pstr), encodeAndEncrypt(email)
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
    val email = SendEmailRequest(
      List(minimalPSAPSP.email),
      "pods_authorise_psp",
      Map(
        "psaInvitor" -> minimalPSAPSP.name,
        "pspInvitee" -> pspName,
        "schemeName" -> schemeName
      ),
      force = false,
      eventUrl = Some(callBackUrl(psaId, pspId, pstr, minimalPSAPSP.email))
    )

    emailConnector.sendEmail(email).map { emailStatus =>
      if (emailStatus == EmailNotSent) {
        Logger.error("Unable to send email to authorising PSA. Support intervention possibly required.")
      }
      ()
    }

  }
}
