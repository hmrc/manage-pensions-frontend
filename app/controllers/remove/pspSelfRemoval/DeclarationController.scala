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

package controllers.remove.pspSelfRemoval

import com.google.inject.Inject
import connectors.admin.MinimalConnector
import connectors.{EmailConnector, EmailNotSent, PspConnector}
import controllers.Retrievals
import controllers.actions.{DataRequiredAction, AuthAction, DataRetrievalAction}
import forms.remove.RemovePspDeclarationFormProvider
import identifiers.invitations.PSTRId
import identifiers.remove.pspSelfRemoval.RemovalDateId
import identifiers.{SchemeNameId, SchemeSrnId, AuthorisedPractitionerId}
import models.AuthEntity.PSP
import models.{MinimalPSAPSP, SendEmailRequest}
import models.invitations.psp.DeAuthorise
import models.requests.DataRequest
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Action}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.remove.pspSelfRemoval.declaration

import scala.concurrent.{Future, ExecutionContext}

class DeclarationController @Inject()(override val messagesApi: MessagesApi,
                                      formProvider: RemovePspDeclarationFormProvider,
                                      auth: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      pspConnector: PspConnector,
                                      minimalConnector: MinimalConnector,
                                      emailConnector: EmailConnector,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: declaration
                                     )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Retrievals {
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
                  pspConnector.deAuthorise(pstr, deAuthModel).flatMap { _ =>
                    minimalConnector.getMinimalPspDetails(pspId).flatMap { minimalPSAPSP =>
                      sendEmail(minimalPSAPSP, authorisedPractitioner.authorisingPSA.name, schemeName).map { _ =>
                        Redirect(controllers.remove.pspSelfRemoval.routes.ConfirmationController.onPageLoad())
                      }
                    }
                  }
                }
              )
        }
  }

  private def sendEmail(
    minimalPSP: MinimalPSAPSP,
    psaName: String,
    schemeName: String
  )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Unit] = {
    val emailTemplateId =
      s"pods_psp_de_auth_psp_${minimalPSP.individualDetails.fold("company_partnership")(_=>"individual")}"
    val sendEmailRequest = SendEmailRequest(
      List(minimalPSP.email),
      emailTemplateId,
      Map(
        "authorisingPsaName" -> psaName,
        "pspName" -> minimalPSP.name,
        "schemeName" -> schemeName
      ),
      force = false,
      eventUrl = None
    )

    emailConnector.sendEmail(sendEmailRequest).map { emailStatus =>
      if (emailStatus == EmailNotSent) {
        logger.error("Unable to send email to PSP. Support intervention possibly required.")
      }
      ()
    }

  }
}
