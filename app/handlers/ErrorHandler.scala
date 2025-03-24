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

package handlers

import com.google.inject.Inject
import connectors.admin.{DelimitedAdminException, DelimitedPractitionerException}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, RequestHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html._

import scala.concurrent.{ExecutionContext, Future}

class ErrorHandler @Inject()(
                              val messagesApi: MessagesApi,
                              view: error_template,
                              notFoundView: error_template_page_not_found
                            )(implicit val ec: ExecutionContext) extends FrontendErrorHandler with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: RequestHeader): Future[Html] =
    Future.successful(view(pageTitle, heading, message)(Request(request, ""), messagesApi.preferred(request)))

  def notFoundTemplate(implicit request: Request[_]): Html =
    notFoundView()


  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception match {
      case _: DelimitedPractitionerException => Future.successful(Redirect(controllers.routes.DelimitedAdministratorController.pspOnPageLoad))
      case _: DelimitedAdminException => Future.successful(Redirect(controllers.routes.DelimitedAdministratorController.onPageLoad))
      case _ => super.onServerError(request, exception)
    }

  }
}
