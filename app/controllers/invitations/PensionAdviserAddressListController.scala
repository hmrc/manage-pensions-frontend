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

package controllers.invitations

import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import javax.inject.Inject
import models.Mode
import play.api.mvc.{Action, AnyContent}

class PensionAdviserAddressListController @Inject()(
                                                   val authenticate: AuthAction,
                                                   val getData: DataRetrievalAction,
                                                   val requireData: DataRequiredAction,
                                                   val formProvider: PensionAdviserAddressListFormProvider
                                                   ) {

  def onPageLoad(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>
      ???
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>
      ???
  }

}
