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

package testhelpers

import models.{invitations, _}
import models.invitations.Invitation
import uk.gov.hmrc.domain.PsaId

import java.time.LocalDate

object InvitationBuilder {
  val unit: Unit = ()
  val srn = SchemeReferenceNumber("S0987654321")
  val pstr1 = "S12345"
  val inviteePsaId1 = PsaId("P1234567")
  val inviterPsaId1 = PsaId("I1234567")
  val invitation1: Invitation = {
    val schemeName1 = "Test scheme1 name"
    val inviteeName1 = "Test Invitee1 Name"
    val expiryDate1 = LocalDate.parse("2018-11-10").atStartOfDay()

    invitations.Invitation(srn, pstr1, schemeName1, inviterPsaId1, inviteePsaId1, inviteeName1, expiryDate1)

  }
  val invitation2: Invitation = {
    val pstr2 = "D1234"
    val schemeName2 = "Test scheme2 name"
    val inviterPsaId2 = PsaId("Q1234567")
    val inviteePsaId2 = PsaId("T1234567")
    val inviteeName2 = "Test Invitee2 Name"
    val expiryDate2 = LocalDate.parse("2018-11-11").atStartOfDay()

    invitations.Invitation(srn, pstr2, schemeName2, inviterPsaId2, inviteePsaId2, inviteeName2, expiryDate2)

  }
  val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), postcode = Some("AB11AB"), country = "GB")
  val pensionAdviser = PensionAdviserDetails(
    name = "test adviser",
    addressDetail = address,
    email = "test@test.com"
  )

  val acceptedInvitation = AcceptedInvitation(
    pstr = pstr1,
    inviteePsaId = PsaId("A0000000"),
    inviterPsaId = inviterPsaId1,
    declaration = true,
    declarationDuties = false,
    pensionAdviserDetails = Some(pensionAdviser)
  )
  val invitationList = List(invitation1, invitation2)


}

