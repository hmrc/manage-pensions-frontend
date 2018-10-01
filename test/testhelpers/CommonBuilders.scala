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

package testhelpers

import models._

object CommonBuilders {

  private val address = Address("Telford1", "Telford2", Some("Telford13"), Some("Telford14"), Some("TF3 4ER"), "GB")
  val correspondenceAddress = CorrespondenceAddress("Telford1", "Telford2", Some("Telford13"), Some("Telford14"), "GB", Some("TF3 4ER"))
  private val indEstAddress = Address("addressline1","addressline2",Some("addressline3"),Some("addressline4"),Some("TF3 5TR"),"GB")
  private val comEstAddress = Address("line1","line2",Some("line3"),Some("line4"),Some("LE45RT"),"GB")
  private val contactDetails = ContactDetails("0044-09876542312", Some("0044-09876542312"), Some("0044-09876542312"), "abc@hmrc.gsi.gov.uk")
  private val indEstcontactDetails = ContactDetails("0044-09876542334",Some("0044-09876542312"),Some("0044-09876542312"),"aaa@gmail.com")
  private val comEstcontactDetails = ContactDetails("0044-09876542312",Some("0044-09876542312"),Some("0044-09876542312"),"abcfe@hmrc.gsi.gov.uk")
  private val indEstPrevAdd = PreviousAddressDetails(true, Some(Address("sddsfsfsdf","sddsfsdf",Some("sdfdsfsdf"),Some("sfdsfsdf"),Some("456546"),"AD")))
  private val comEstPrevAdd = PreviousAddressDetails(true,Some(Address("addline1","addline2",Some("addline3"),Some("addline4"),Some("ST36TR"),"AD")))
  //private val personDetails = PersonDetails(Some("Mr"),"abcdef",Some("fdgdgfggfdg"),"dfgfdgdfg","1955-03-29")

  val schemeDetails = SchemeDetails(Some("S9000000000"), Some("00000000AA"), "Open", "Benefits Scheme", Some(true),
    Some("A single trust under which all of the assets are held for the benefit of all members of the scheme"),
    Some(" "), true, SchemeMemberNumbers("0", "0"), true, true, "Money Purchase benefits only (defined contribution)", "AD", true,
    Some(InsuranceCompany(Some("Aviva Insurance"), Some(" "), Some(correspondenceAddress))))

  //private val indEstablisher = Individual(personDetails,Some("AA999999A"),Some("retxgfdg"),Some("1234567892"),
    //Some("asdgdgdsg"),indEstAddress,indEstcontactDetails,indEstPrevAdd)

  /*private val compEstablisher = CompanyEstablisher("abc organisation",Some("7897700000"),Some("reason forutr"),Some("sdfsfs"),
    Some("crn no reason"),Some("789770000"),Some("9999"),Some(true),comEstAddress,comEstcontactDetails,Some(comEstPrevAdd),None)*/

  //private val establisherDetails = EstablisherDetails(Some(List(indEstablisher)), Some(List(compEstablisher)), None)

  val psaDetails1 = PsaDetails("A0000000",Some("partnetship name"),Some("Taylor"),Some("Middle"),Some("Rayon"),Some("Primary"),Some("1978-03-22"))
  val psaDetails2 = PsaDetails("A0000001",Some("partnetship name 1"),Some("Smith"),Some("A"),Some("Tony"),Some("Primary"),Some("1977-03-22"))

  //val psaSchemeDetailsResponse = PsaSchemeDetails(PensionsScheme(schemeDetails, Some(establisherDetails), None, Some(Seq(psaDetails1, psaDetails2))))
  //val schemeDetailsWithPsaOnlyResponse = PsaSchemeDetails(PensionsScheme(schemeDetails, None, None, Some(Seq(psaDetails1, psaDetails2))))
  //val schemeDetailsPendingResponse = PsaSchemeDetails(PensionsScheme(schemeDetails.copy(schemeStatus = "Pending"), None, None, Some(Seq(psaDetails1, psaDetails2))))
  //val schemeDetailsWithoutPsaResponse = PsaSchemeDetails(PensionsScheme(schemeDetails, None, None, None))


  private val schemeDetail = SchemeDetail("abcdefghi", "S1000000456", "Pending", Some("2012-10-10"),
    Some("10000678RE"), Some("Primary PSA"), None)

  private val schemeDetailWithoutDate = SchemeDetail("abcdefghi", "S1000000456", "Pending", None,
    Some("10000678RE"), Some("Primary PSA"), None)

  val listOfSchemesResponse = ListOfSchemes("2001-12-17T09:30:47Z", "1", Some(List(schemeDetail)))
  val listOfSchemesPartialResponse = ListOfSchemes("2001-12-17T09:30:47Z", "1", Some(List(schemeDetailWithoutDate)))

}
