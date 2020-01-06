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

package controllers.testonly

import com.google.inject.Inject
import connectors.MongoDiagnosticsConnector
import controllers.actions.AuthAction
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class MongoDiagnosticsController @Inject()(connector: MongoDiagnosticsConnector, authenticate: AuthAction,
                                           val controllerComponents: MessagesControllerComponents)(
  implicit val ec: ExecutionContext) extends FrontendBaseController {

  // scalastyle:off magic.number
  private val banner = Seq.fill(50)("-").mkString
  // scalastyle:on magic.number

  def mongoDiagnostics(): Action[AnyContent] = authenticate.async {
    implicit request =>

      connector.fetchDiagnostics().map {
        diagnostics =>
          Ok(
            Seq(
              banner,
              "Session",
              banner,
              s"External Id: ${request.externalId}",
              "",
              diagnostics
            ).mkString("\n")
          )
      }

  }

}
