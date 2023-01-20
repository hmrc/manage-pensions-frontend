/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{SendEmailRequest, URBanner}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.banner

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
                                  view: banner
                               )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  private val form: Form[URBanner] = formProvider()

  def onPageLoad: Action[AnyContent] = authenticate().async {
    implicit request =>
      val psaId = request.psaIdOrException.id
      minConnector.getMinimalPsaDetails(psaId).map {
        minDetails =>
          val name = if (minDetails.individualDetails.isDefined) minDetails.name else ""
          val fm = form.fill(
            URBanner(
              name,
              minDetails.email))
          Ok(view(fm))
      }
  }

  def onSubmit: Action[AnyContent] = authenticate().async {
    implicit request =>
      val psaId = request.psaIdOrException.id
      form.bindFromRequest().fold(
        (formWithErrors: Form[URBanner]) =>
          Future.successful(BadRequest(view(formWithErrors))),
        value => {
          for {
            minDetails <- minConnector.getMinimalPsaDetails(psaId)
            email <- emailConnector.sendEmail(SendEmailRequest.apply(
              to = List("david.saunders@digital.hmrc.gov.uk"),
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
            Ok(view(form))
          }
          Future.successful(Redirect(controllers.routes.BannerConfirmationController.onPageLoad))
        }
      )
  }
}
