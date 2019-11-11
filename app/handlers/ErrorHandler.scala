/*
 * Copyright 2019 HM Revenue & Customs
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

import config.FrontendAppConfig
import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler
import views.html.error_template
import views.html.error_template_page_not_found

import scala.language.implicitConversions

@Singleton
class ErrorHandler @Inject()(
                              appConfig: FrontendAppConfig,
                              val messagesApi: MessagesApi
                            ) extends FrontendErrorHandler with I18nSupport {

  override def notFoundTemplate(implicit request: Request[_]): Html = error_template_page_not_found()

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
    error_template(pageTitle, heading, message)
}
