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

package controllers

import config.FrontendAppConfig
import connectors.EmailConnector
import connectors.admin.MinimalConnector
import controllers.actions.AuthAction
import forms.UrBannerFormProvider
import models.AuthEntity.{PSA, PSP}
import models.{SendEmailRequest, URBanner}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{banner_psa, banner_psp}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BannerController @Inject()(
                                  val appConfig: FrontendAppConfig,
                                  formProvider: UrBannerFormProvider,
                                  minConnector: MinimalConnector,
                                  emailConnector: EmailConnector,
                                  override val messagesApi: MessagesApi,
                                  authenticate: AuthAction,
                                  val controllerComponents: MessagesControllerComponents,
                                  viewPsa: banner_psa,
                                  viewPsp: banner_psp
                               )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  private val form: Form[URBanner] = formProvider()

  def onPageLoadPsa: Action[AnyContent] = authenticate(PSA).async {
    implicit request =>
      minConnector.getMinimalPsaDetails().map {
        minDetails =>
          val name = minDetails.name
          val fm = form.fill(
            URBanner(
              name,
              minDetails.email))
          Ok(viewPsa(fm))
      }
  }

  def onPageLoadPsp: Action[AnyContent] = authenticate(PSP).async {
    implicit request =>
      minConnector.getMinimalPspDetails().map {
        minDetails =>
          val name = minDetails.name
          val fm = form.fill(
            URBanner(
              name,
              minDetails.email))
          Ok(viewPsp(fm))
      }
  }

  def onSubmitPsa: Action[AnyContent] = authenticate(PSA).async {
    implicit request =>
      val psaId = request.psaIdOrException.id
      form.bindFromRequest().fold(
        (formWithErrors: Form[URBanner]) =>
          Future.successful(BadRequest(viewPsa(formWithErrors))),
        value => {
          for {
            minDetails <- minConnector.getMinimalPsaDetails()
            _ <- emailConnector.sendEmail(SendEmailRequest.apply(
              to = List(appConfig.urBannerEmail),
              templateId = "pods_user_research_banner",
              parameters = Map(
                "psaName" -> value.indOrgName,
                "comOrgName" -> minDetails.name,
                "psaId" -> psaId,
                "psaEmail" -> value.email
              ),
              eventUrl = None
            ))
          } yield {
            Redirect(controllers.routes.BannerConfirmationController.onPageLoadPsa)
          }
        }
      )
  }

  def onSubmitPsp: Action[AnyContent] = authenticate(PSP).async {
    implicit request =>
      val pspId = request.pspIdOrException.id
      form.bindFromRequest().fold(
        (formWithErrors: Form[URBanner]) =>
          Future.successful(BadRequest(viewPsp(formWithErrors))),
        value => {
          for {
            minDetails <- minConnector.getMinimalPspDetails()
            _ <- emailConnector.sendEmail(SendEmailRequest.apply(
              to = List(appConfig.urBannerEmail),
              templateId = "pods_user_research_banner",
              parameters = Map(
                "psaName" -> value.indOrgName,
                "comOrgName" -> minDetails.name,
                "psaId" -> pspId,
                "psaEmail" -> value.email
              ),
              eventUrl = None
            ))
          } yield {
            Redirect(controllers.routes.BannerConfirmationController.onPageLoadPsp)
          }
        }
      )
  }
}
