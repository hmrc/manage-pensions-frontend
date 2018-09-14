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

package models

import play.api.libs.json.{Format, Json}

case class AcceptedInvitation(
                               pstr: String,
                               inviteePsaId: String,
                               inviterPsaId: String,
                               declaration: Boolean,
                               declarationDuties: Boolean,
                               pensionAdviserDetail: Option[PensionAdviserDetail]
                             )

object AcceptedInvitation {
  implicit val formats: Format[AcceptedInvitation] = Json.format
}

case class PensionAdviserDetail(name: String, addressDetail: Address, contactDetail: ContactDetails)

object PensionAdviserDetail {
  implicit val formats: Format[PensionAdviserDetail] = Json.format
}

