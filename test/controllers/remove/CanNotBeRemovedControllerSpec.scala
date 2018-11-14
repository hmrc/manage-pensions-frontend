/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.remove

import controllers.actions.{AuthAction, DataRetrievalAction, FakeAuthAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.RemovalViewModel
import views.html.remove.cannot_be_removed

import scala.concurrent.{ExecutionContext, Future}

class CanNotBeRemovedControllerSpec extends ControllerWithNormalPageBehaviours {

  import CanNotBeRemovedControllerSpec._


  private def onPageLoadActionIndividual(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new CanNotBeRemovedController(
      frontendAppConfig, messagesApi, fakeAuth, fakeAuthConnector(retrievalResultIndividual)).onPageLoad()
  }

  private def onPageLoadActionOrganisation(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction): Action[AnyContent] = {

    new CanNotBeRemovedController(
      frontendAppConfig, messagesApi, fakeAuth, fakeAuthConnector(retrievalResultOrganisation)).onPageLoad()
  }

  def individualViewAsString(): String = cannot_be_removed(viewModelIndividual, frontendAppConfig)(fakeRequest, messages).toString

  def organisationViewAsString(): String = cannot_be_removed(viewModelOrganisation, frontendAppConfig)(fakeRequest, messages).toString

  "if affinity group Individual" must {

    behave like controllerWithOnPageLoadMethod(onPageLoadActionIndividual, getEmptyData, None, individualViewAsString)
  }

  "if affinity group not Individual" must {

    behave like controllerWithOnPageLoadMethod(onPageLoadActionOrganisation, getEmptyData, None, organisationViewAsString)
  }

  "if affinity group is not present" must {

    "redirect to session expired" in {

      val controller = new CanNotBeRemovedController(
        frontendAppConfig, messagesApi, FakeAuthAction(), fakeAuthConnector(Future.successful(None)))

      val result = controller.onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
    }
  }
}

object CanNotBeRemovedControllerSpec {

  private val viewModelIndividual: RemovalViewModel = RemovalViewModel(
    "messages__you_cannot_be_removed__title",
    "messages__you_cannot_be_removed__heading",
    "messages__you_cannot_be_removed__p1",
    "messages__you_cannot_be_removed__p2",
    "messages__you_cannot_be_removed__returnToSchemes__link")

  private val viewModelOrganisation: RemovalViewModel = RemovalViewModel(
    "messages__psa_cannot_be_removed__title",
    "messages__psa_cannot_be_removed__heading",
    "messages__psa_cannot_be_removed__p1",
    "messages__psa_cannot_be_removed__p2",
    "messages__psa_cannot_be_removed__returnToSchemes__link")

  private val retrievalResultIndividual = Future.successful(Some(AffinityGroup.Individual))

  private val retrievalResultOrganisation = Future.successful(Some(AffinityGroup.Organisation))

  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]): AuthConnector = new AuthConnector {

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      stubbedRetrievalResult.map(_.asInstanceOf[A])
    }
  }

}