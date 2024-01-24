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

package utils

import models.requests.DataRequest
import play.api.mvc.AnyContent
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.PsaId

class FakeDataRequest(request: Request[AnyContentAsEmpty.type], externalId: String, answers: UserAnswers, psaId: PsaId)
  extends DataRequest[AnyContent](request, externalId, answers, Some(psaId))

object FakeDataRequest {
  def apply(answers: UserAnswers): FakeDataRequest = {
    new FakeDataRequest(FakeRequest("", ""), "test-external-id", answers, PsaId("A0000000"))
  }
}
