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

package controllers

import connectors.UserAnswersCacheConnector
import connectors.aft.AftCacheConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class LogoutControllerSpec extends ControllerSpecBase with MockitoSugar {
  private val mockUserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val mockAftCacheConnector = mock[AftCacheConnector]

  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]): AuthConnector = new AuthConnector {

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])
      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      stubbedRetrievalResult.map(_.asInstanceOf[A])(ec)
  }

  private def logoutController = new LogoutController(
    fakeAuthConnector(Future.successful(Some("id"))),
    appConfig = frontendAppConfig,
    aftCacheConnector = mockAftCacheConnector,
    controllerComponents = controllerComponents,
    mockUserAnswersCacheConnector
  )

  "Logout Controller" must {

    "redirect to feedback survey page for an Individual and remove all items from mongo cache" in {
      when(mockUserAnswersCacheConnector.removeAll(any())(any(), any())).thenReturn(Future.successful(Ok))
      when(mockAftCacheConnector.removeLock(any(), any())).thenReturn(Future.successful(Ok))
      val result = logoutController.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(frontendAppConfig.serviceSignOut)
    }
  }
}
