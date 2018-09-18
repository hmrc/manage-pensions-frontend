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

package utils

import models._

trait MockDataHelper {

  protected val address = Address(false, "Telford1", "Telford2", Some("Telford13"), Some("Telford14"), Some("TF3 4ER"), "GB")
  protected val indEstAddress = Address(false,"addressline1","addressline2",Some("addressline3"),Some("addressline4"),Some("TF3 5TR"),"GB")
  protected val comEstAddress = Address(true,"line1","line2",Some("line3"),Some("line4"),Some("LE45RT"),"GB")
  protected val contactDetails = ContactDetails("0044-09876542312", Some("0044-09876542312"), Some("0044-09876542312"), "abc@hmrc.gsi.gov.uk")
  protected val indEstcontactDetails = ContactDetails("0044-09876542334",Some("0044-09876542312"),Some("0044-09876542312"),"aaa@gmail.com")
  protected val comEstcontactDetails = ContactDetails("0044-09876542312",Some("0044-09876542312"),Some("0044-09876542312"),"abcfe@hmrc.gsi.gov.uk")
  protected val indEstPrevAdd = PreviousAddressDetails(true, Some(Address(true,"sddsfsfsdf","sddsfsdf",Some("sdfdsfsdf"),Some("sfdsfsdf"),Some("456546"),"AD")))
  protected val comEstPrevAdd = PreviousAddressDetails(true,Some(Address(true,"addline1","addline2",Some("addline3"),Some("addline4"),Some("ST36TR"),"AD")))
  protected val personDetails = PersonDetails(Some("Mr"),"abcdef",Some("fdgdgfggfdg"),"dfgfdgdfg","1955-03-29")

}
