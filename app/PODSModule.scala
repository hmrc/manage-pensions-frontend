/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.AbstractModule
import controllers.actions.{AuthActionImpl, AuthAction, AuthActionNoAdministratorOrPractitionerCheckImpl}
import utils.Navigator
import utils.annotations._
import utils.countryOptions.{CountryOptions, CountryOptionsEUAndEEA}
import utils.navigators._

class PODSModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Navigator])
      .annotatedWith(classOf[Invitation])
      .to(classOf[InvitationNavigator])

    bind(classOf[Navigator])
      .annotatedWith(classOf[Triage])
      .to(classOf[TriageNavigator])

    bind(classOf[Navigator])
      .annotatedWith(classOf[AcceptInvitation])
      .to(classOf[AcceptInvitationNavigator])

    bind(classOf[Navigator])
      .annotatedWith(classOf[AuthorisePsp])
      .to(classOf[AuthorisePspNavigator])

    bind(classOf[Navigator])
      .annotatedWith(classOf[RemovePSA])
      .to(classOf[RemovePSANavigator])

    bind(classOf[Navigator])
      .annotatedWith(classOf[RemovePSP])
      .to(classOf[RemovePSPNavigator])

    bind(classOf[Navigator])
      .annotatedWith(classOf[PspSelfRemoval])
      .to(classOf[PspSelfRemovalNavigator])

    bind(classOf[Navigator])
      .to(classOf[ManageNavigator])

    bind(classOf[AuthAction])
      .annotatedWith(classOf[NoAdministratorOrPractitionerCheck])
      .to(classOf[AuthActionNoAdministratorOrPractitionerCheckImpl])

    bind(classOf[AuthAction])
      .to(classOf[AuthActionImpl])

    bind(classOf[CountryOptions])
      .annotatedWith(classOf[EUAndEEA])
      .to(classOf[CountryOptionsEUAndEEA])
  }
}
