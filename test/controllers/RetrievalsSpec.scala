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

package controllers

import identifiers.TypedIdentifier
import models.requests.DataRequest
import org.scalatest.EitherValues
import org.scalatest.WordSpec
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers

import scala.concurrent.Future

class RetrievalsSpec extends WordSpec with FrontendBaseController with Retrievals with EitherValues with ScalaFutures {

  val controllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()

  def dataRequest(data: JsValue): DataRequest[AnyContent] = DataRequest(FakeRequest("", ""), "cacheId", UserAnswers(data), Some(PsaId("A0000000")))

  val success: String => Future[Result] = { _: String =>
    Future.successful(Ok("Success"))
  }

  val testIdentifier: TypedIdentifier[String] = new TypedIdentifier[String] {
    override def toString: String = "test"
  }

  val secondIdentifier: TypedIdentifier[String] = new TypedIdentifier[String] {
    override def toString: String = "second"
  }

  "retrieve" must {

    "reach the intended result when identifier gets value from answers" in {

      implicit val request: DataRequest[AnyContent] = dataRequest(Json.obj("test" -> "result"))

      testIdentifier.retrieve.right.value mustEqual "result"
    }

    "reach the intended result when identifier uses and to get the value from answers" in {

      implicit val request: DataRequest[AnyContent] = dataRequest(Json.obj("test" -> "result", "second" -> "answer"))

      (testIdentifier and secondIdentifier).retrieve.right.value mustEqual new ~("result", "answer")
    }

    "redirect to the session expired page when cant find identifier" in {

      implicit val request: DataRequest[AnyContent] = dataRequest(Json.obj("test1" -> "result"))

      whenReady(testIdentifier.retrieve.left.value) {
        _ mustEqual Redirect(routes.SessionExpiredController.onPageLoad())
      }
    }

  }

}
