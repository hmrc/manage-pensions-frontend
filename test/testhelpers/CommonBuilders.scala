/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.LocalDate
import models._
import models.psa.{Name, PreviousAddressDetails, PsaDetails}

object CommonBuilders {

  val address = Address("Telford1", "Telford2", Some("Telford13"), Some("Telford14"), Some("TF3 4ER"), "GB")
  val indEstAddress = Address("addressline1","addressline2",Some("addressline3"),Some("addressline4"),Some("TF3 5TR"),"GB")
  val comEstAddress = Address("line1","line2",Some("line3"),Some("line4"),Some("LE45RT"),"GB")
  val contactDetails = ContactDetails("0044-09876542312", Some("0044-09876542312"), Some("0044-09876542312"), "abc@hmrc.gsi.gov.uk")
  val indEstcontactDetails = ContactDetails("0044-09876542334",Some("0044-09876542312"),Some("0044-09876542312"),"aaa@gmail.com")
  val comEstcontactDetails = ContactDetails("0044-09876542312",Some("0044-09876542312"),Some("0044-09876542312"),"abcfe@hmrc.gsi.gov.uk")
  val indEstPrevAdd = PreviousAddressDetails(true, Some(Address("sddsfsfsdf","sddsfsdf",Some("sdfdsfsdf"),Some("sfdsfsdf"),Some("456546"),"AD")))
  val comEstPrevAdd = PreviousAddressDetails(true,Some(Address("addline1","addline2",Some("addline3"),Some("addline4"),Some("ST36TR"),"AD")))

  val psaDetails1: PsaDetails = PsaDetails("A0000000",Some("partnetship name"),Some(Name(Some("Taylor"),Some("Middle"),Some("Rayon"))), Some("2018-10-01"))
  val psaDetails2 = PsaDetails("A0000001",Some("partnetship name 1"),Some(Name(Some("Smith"),Some("A"),Some("Tony"))), Some("2018-10-02"))
  val psaDetails3 = PsaDetails("A0000000",Some("partnetship name 2"),None, None)


  val schemeDetail = SchemeDetails("abcdefghi", "S1000000456", SchemeStatus.Pending.value, Some("2012-10-10"),
    Some("10000678RE"), Some("Primary PSA"), None)

  val schemeDetailWithoutDate = SchemeDetails("abcdefghi", "S1000000456", SchemeStatus.Pending.value, None,
    Some("10000678RE"), Some("Primary PSA"), None)

  val listOfSchemesResponse = ListOfSchemes("2001-12-17T09:30:47Z", "1", Some(List(schemeDetail)))
  val listOfSchemesPartialResponse = ListOfSchemes("2001-12-17T09:30:47Z", "1", Some(List(schemeDetailWithoutDate)))

  private val pspName: String = "test-psp-name"
  val psaName: String = "test-psa-name"
  private val authorisingPsa: AuthorisingPSA = AuthorisingPSA(None, None, None, Some(psaName))
  val pspDetailsSeq: Seq[AuthorisedPractitioner] = Seq(
    AuthorisedPractitioner(Some("A0000000"),Some(pspName),None, "A0000000", authorisingPsa,
      LocalDate.parse("2020-04-01"), "00000000")
  )

  val pspDetails: AuthorisedPractitioner = AuthorisedPractitioner(
    clientReference=Some("A0000000"),
    organisationOrPartnershipName = Some(pspName),
    individual = None,
    authorisingPSAID = "A0000000",
    authorisingPSA = authorisingPsa,
    relationshipStartDate = LocalDate.parse("2020-04-01"),
    id = "00000000"
  )
  val pspDetails2: AuthorisedPractitioner = AuthorisedPractitioner(
    clientReference=Some("A0000000"),
    organisationOrPartnershipName = Some(pspName),
    individual = None,
    authorisingPSAID = "A0000001",
    authorisingPSA = authorisingPsa,
    relationshipStartDate = LocalDate.parse("2020-04-01"),
    id = "00000000"
  )
}
