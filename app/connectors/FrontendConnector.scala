/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.mvc.Request
import play.twirl.api.Html
import services.HeaderCarrierFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.play.partials.HtmlPartial.connectionExceptionsAsHtmlPartialFailure

import scala.concurrent.{ExecutionContext, Future}

class FrontendConnector @Inject()(http: HttpClient, config: FrontendAppConfig) {

  def retrieveAftPartial[A](srn: String)(implicit request: Request[A], ec: ExecutionContext): Future[Html] =
    retrievePartial(config.aftPartialHtmlUrl.format(srn))


  private def retrievePartial[A](url: String)(implicit request: Request[A], ec: ExecutionContext): Future[Html] = {
    implicit val hc: HeaderCarrier = HeaderCarrierFunctions.headerCarrierForPartials(request).toHeaderCarrier

    http.GET[HtmlPartial](url) recover connectionExceptionsAsHtmlPartialFailure map {
      case HtmlPartial.Success(_, content) =>
        content
      case HtmlPartial.Failure(_, _) =>
        Html("Sorry, there's been a technical problem retrieving your info")
    }
  }

}
