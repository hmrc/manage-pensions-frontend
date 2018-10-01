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
  private val indEstAddress = Address("addressline1","addressline2",Some("addressline3"),Some("addressline4"),Some("TF3 5TR"),"GB")
  private val comEstAddress = Address("line1","line2",Some("line3"),Some("line4"),Some("LE45RT"),"GB")
  private val contactDetails = ContactDetails("0044-09876542312", Some("0044-09876542312"), Some("0044-09876542312"), "abc@hmrc.gsi.gov.uk")
  private val indEstcontactDetails = ContactDetails("0044-09876542334",Some("0044-09876542312"),Some("0044-09876542312"),"aaa@gmail.com")
  private val comEstcontactDetails = ContactDetails("0044-09876542312",Some("0044-09876542312"),Some("0044-09876542312"),"abcfe@hmrc.gsi.gov.uk")
  private val indEstPrevAdd = PreviousAddressDetails(true, Some(Address("sddsfsfsdf","sddsfsdf",Some("sdfdsfsdf"),Some("sfdsfsdf"),Some("456546"),"AD")))
  private val comEstPrevAdd = PreviousAddressDetails(true,Some(Address("addline1","addline2",Some("addline3"),Some("addline4"),Some("ST36TR"),"AD")))


}
