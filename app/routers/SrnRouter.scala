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

package routers

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

import models.SchemeReferenceNumber
import play.api.http.Status.NOT_FOUND
import play.api.libs.typedmap.TypedKey
import play.api.mvc._
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.mvc.Action
import routers.SrnRouter.srnTypedKey

import javax.inject.Inject
import scala.util.{Success, Try}
class SrnRouter @Inject() (srnRoutes: srn.Routes, errorHandler: play.api.http.HttpErrorHandler, Action: DefaultActionBuilder) extends SimpleRouter {

  override def routes: Routes = {
    case requestHeader =>
      def handleWrongPath = Action.async {
        errorHandler.onClientError(requestHeader, NOT_FOUND, "URL not found")
      }
      val splitPath = requestHeader.path.split("/")
      val srnString = Try(splitPath(1))
      srnString match {
        case Success(srn) if SchemeReferenceNumber.srnValid(srn) =>
          def modifyRequestFunc(requestHeader: RequestHeader) = requestHeader.addAttr(srnTypedKey, SchemeReferenceNumber(srn))
          val srnPathPrefix = "/" + srn + "/"
          val srnRoutesPf = srnRoutes.withPrefix(srnPathPrefix).routes

          if(srnRoutesPf.isDefinedAt(requestHeader))
            Handler.Stage.modifyRequest(
              modifyRequestFunc,
              srnRoutesPf(requestHeader)
            )
          else {
            handleWrongPath
          }
        case _ => handleWrongPath

      }
  }
}

object SrnRouter {
  val srnTypedKey: TypedKey[SchemeReferenceNumber] = TypedKey("srn")
}