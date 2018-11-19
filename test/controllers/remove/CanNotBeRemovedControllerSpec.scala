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

import controllers.actions.{FakeAuthAction, FakeUnAuthorisedAction}
import controllers.behaviours.ControllerWithNormalPageBehaviours
import models.{Individual, Organization, OtherUser}
import play.api.test.Helpers.{status, _}
import viewmodels.RemovalViewModel
import views.html.remove.cannot_be_removed

class CanNotBeRemovedControllerSpec extends ControllerWithNormalPageBehaviours {

  import CanNotBeRemovedControllerSpec._

  val fakeControllerAction = new CanNotBeRemovedController(
    frontendAppConfig, messagesApi, FakeUnAuthorisedAction())

  def individualViewAsString(): String = cannot_be_removed(viewModelIndividual, frontendAppConfig)(fakeRequest, messages).toString

  def organisationViewAsString(): String = cannot_be_removed(viewModelOrganisation, frontendAppConfig)(fakeRequest, messages).toString

  "if affinity group Individual" must {

    "return OK and the correct view for a GET" in {

      val controller = new CanNotBeRemovedController(
        frontendAppConfig, messagesApi, FakeAuthAction.createUserType(Individual))

      val result = controller.onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe individualViewAsString()
    }
  }

  "if affinity group Organization" must {

    "return OK and the correct view for a GET" in {

      val controller = new CanNotBeRemovedController(
        frontendAppConfig, messagesApi, FakeAuthAction.createUserType(Organization))

      val result = controller.onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe organisationViewAsString()
    }
  }

  "if affinity group is not Individual or Organization" must {

    "redirect to session expired" in {

      val controller = new CanNotBeRemovedController(
        frontendAppConfig, messagesApi, FakeAuthAction.createUserType(OtherUser))

      val result = controller.onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
    }

    "return 303 if user action is not authenticated" in {

      val result = fakeControllerAction.onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad().url)
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
}
