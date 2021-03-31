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

package controllers

import audit._
import com.google.inject.Inject
import models.{Opened, EmailEvents}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted}
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext.Implicits.global

class EmailResponseController @Inject()(
                                         auditService: AuditService,
                                         cc: ControllerComponents,
                                         crypto: ApplicationCrypto,
                                         parser: PlayBodyParsers,
                                         val controllerComponents: MessagesControllerComponents,
                                         val authConnector: AuthConnector
                                       )
  extends FrontendBaseController
    with AuthorisedFunctions {

  import EmailResponseController._

  private val logger = Logger(classOf[EmailResponseController])

  def retrieveStatusForPSPAuthorisation(
                                         encryptedPsaId: String,
                                         encryptedPspId: String,
                                         encryptedPstr: String,
                                         encryptedEmail: String
                                       ): Action[JsValue] = Action(parser.tolerantJson) {
    implicit request =>
      decryptAndValidateDetailsForPSPAuthAndDeauth(encryptedPsaId, encryptedPspId, encryptedPstr, encryptedEmail) match {
        case Right(Tuple4(psaId, pspId, pstr, email)) =>
          request.body.validate[EmailEvents].fold(
            _ => BadRequest("Bad request received for psp authorisation email call back event"),
            valid => {
              valid.events.filterNot(
                _.event == Opened
              ).foreach { event =>
                logger.debug(s"Email Audit event is $event")
                auditService.sendEvent(PSPAuthorisationEmailAuditEvent(psaId.id, pspId.id, pstr, email, event.event))
              }
              Ok
            }
          )
        case Left(result) => result
      }
  }

  def retrieveStatusForPSPDeauthorisation(
                                           encryptedPsaId: String,
                                           encryptedPspId: String,
                                           encryptedPstr: String,
                                           encryptedEmail: String
                                         ): Action[JsValue] = Action(parser.tolerantJson) {
    implicit request =>
      decryptAndValidateDetailsForPSPAuthAndDeauth(encryptedPsaId, encryptedPspId, encryptedPstr, encryptedEmail) match {
        case Right(Tuple4(psaId, pspId, pstr, email)) =>
          request.body.validate[EmailEvents].fold(
            _ => BadRequest("Bad request received for psp de-authorisation email call back event"),
            valid => {
              valid.events.filterNot(
                _.event == Opened
              ).foreach { event =>
                logger.debug(s"Email Audit event is $event")
                auditService.sendEvent(PSPDeauthorisationEmailAuditEvent(psaId.id, pspId.id, pstr, email, event.event))
              }
              Ok
            }
          )
        case Left(result) => result
      }
  }

  def retrieveStatusForPSPSelfDeauthorisation(
    encryptedPspId: String,
    encryptedPstr: String,
    encryptedEmail: String
  ): Action[JsValue] = Action(parser.tolerantJson) {
    implicit request =>
      decryptAndValidateDetailsForPSPSelfDeauth(encryptedPspId, encryptedPstr, encryptedEmail) match {
        case Right(Tuple3(pspId, pstr, email)) =>
          request.body.validate[EmailEvents].fold(
            _ => BadRequest("Bad request received for psp self-de-authorisation email call back event"),
            valid => {
              valid.events.filterNot(
                _.event == Opened
              ).foreach { event =>
                logger.debug(s"Email Audit event is $event")
                auditService.sendEvent(PSPSelfDeauthorisationEmailAuditEvent(pspId.id, pstr, email, event.event))
              }
              Ok
            }
          )
        case Left(result) => result
      }
  }


  private def decryptAndValidateDetailsForPSPAuthAndDeauth(
                                                            encryptedPsaId: String,
                                                            encryptedPspId: String,
                                                            encryptedPstr: String,
                                                            encryptedEmail: String): Either[Result, (PsaId, PspId, String, String)] = {

    val psaId = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedPsaId)).value
    val pspId = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedPspId)).value
    val pstr = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedPstr)).value
    val emailAddress = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedEmail)).value

    try {
      require(emailAddress.matches(emailRegex))
      Right(Tuple4(PsaId(psaId), PspId(pspId), pstr, emailAddress))
    } catch {
      case _: IllegalArgumentException => Left(Forbidden(s"Malformed PSAID: $psaId, PSPID: $pspId, PSTR: $pstr or Email: $emailAddress"))
    }
  }

  private def decryptAndValidateDetailsForPSPSelfDeauth(
    encryptedPspId: String,
    encryptedPstr: String,
    encryptedEmail: String): Either[Result, (PspId, String, String)] = {

    val pspId = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedPspId)).value
    val pstr = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedPstr)).value
    val emailAddress = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedEmail)).value

    try {
      require(emailAddress.matches(emailRegex))
      Right(Tuple3(PspId(pspId), pstr, emailAddress))
    } catch {
      case _: IllegalArgumentException => Left(Forbidden(s"Malformed PSPID: $pspId, PSTR: $pstr or Email: $emailAddress"))
    }
  }
}

object EmailResponseController {
  private val emailRegex: String = "^(?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"" +
    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")" +
    "@(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?|" +
    "\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-zA-Z0-9-]*[a-zA-Z0-9]:" +
    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$"
}
