/*
 * Copyright 2022 HM Revenue & Customs
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

package utils.navigators

import base.SpecBase
import identifiers.Identifier
import identifiers.psp.SearchPSTRId
import models.requests.IdentifiedRequest
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import utils.{NavigatorBehaviour, UserAnswers}

class SearchNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour {

  import SearchNavigatorSpec._

  val navigator = new SearchNavigator

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id",                            "User Answers",     "Next Page (NormalMode)",     "Next Page (CheckMode)"),
    (SearchPSTRId,                     emptyAnswers,       pspIdPage,                    None
  ))

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object SearchNavigatorSpec extends OptionValues {
  lazy val emptyAnswers:UserAnswers = UserAnswers(Json.obj())
  lazy val pspIdPage: Call = controllers.psp.routes.ListSchemesController.onSearch()

  implicit val ex: IdentifiedRequest = new IdentifiedRequest() {
    val externalId: String = "test-external-id"
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def dataDescriber(answers: UserAnswers): String = answers.toString

}
