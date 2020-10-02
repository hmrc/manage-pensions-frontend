/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.Format
import play.api.libs.json.Json

case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean, previousAddress: Option[Address])

object PreviousAddressDetails {
  implicit val formats: Format[PreviousAddressDetails] = Json.format[PreviousAddressDetails]
}

case class SubscriptionDetails(psaSubscriptionDetails:PsaSubscriptionDetails)

object SubscriptionDetails {
  implicit val format: Format[SubscriptionDetails] = Json.format[SubscriptionDetails]
}

case class PsaSubscriptionDetails(isPSASuspension:Boolean,
                                  customerIdentificationDetails:CustomerIdentificationDetails,
                                  organisationOrPartnerDetails:Option[OrganisationOrPartnerDetails],
                                  individualDetails:Option[IndividualDetails],
                                  correspondenceAddressDetails:Address,
                                  correspondenceContactDetails:ContactDetails,
                                  previousAddressDetails:PreviousAddressDetails,
                                  numberOfDirectorsOrPartnersDetails:Option[NumberOfDirectorsOrPartners],
                                  directorOrPartnerDetails:Option[List[DirectorOrPartnerDetails]],
                                  declarationDetails:PensionSchemeAdministratorDeclaration)

object PsaSubscriptionDetails {
  implicit val format: Format[PsaSubscriptionDetails] = Json.format[PsaSubscriptionDetails]
}

case class CustomerIdentificationDetails(legalStatus:String,
                                         idType:Option[String],
                                         idNumber:Option[String],
                                         noIdentifier:Boolean)

object CustomerIdentificationDetails {
  implicit val format: Format[CustomerIdentificationDetails] = Json.format[CustomerIdentificationDetails]
}

case class OrganisationOrPartnerDetails(name:String,
                                        crnNumber:Option[String],
                                        vatRegistrationNumber:Option[String],
                                        payeReference:Option[String])

object OrganisationOrPartnerDetails {
  implicit val format: Format[OrganisationOrPartnerDetails] = Json.format[OrganisationOrPartnerDetails]
}

case class DirectorOrPartnerDetails(sequenceId:String,
                                    entityType:String,
                                    title:Option[String],
                                    firstName:String,
                                    middleName:Option[String],
                                    lastName:String,
                                    dateOfBirth:String,
                                    nino:Option[String],
                                    noNinoReason:Option[String],
                                    utr:Option[String],
                                    noUtrReason:Option[String],
                                    correspondenceCommonDetails:Option[CorrespondenceCommonDetails],
                                    previousAddressDetails:PreviousAddressDetails)

object DirectorOrPartnerDetails {
  implicit val format: Format[DirectorOrPartnerDetails] = Json.format[DirectorOrPartnerDetails]
}

case class PensionSchemeAdministratorDeclaration(box1:Boolean,
                                                 box2:Boolean,
                                                 box3:Boolean,
                                                 box4:Boolean,
                                                 box5:Option[Boolean],
                                                 box6:Option[Boolean],
                                                 box7:Boolean,
                                                 pensionAdvisorDetails:Option[PensionAdvisorDetails])

object PensionSchemeAdministratorDeclaration {
  implicit val format: Format[PensionSchemeAdministratorDeclaration] = Json.format[PensionSchemeAdministratorDeclaration]
}

case class NumberOfDirectorsOrPartners(isMorethanTenDirectors:Option[Boolean],
                                       isMorethanTenPartners:Option[Boolean])


object NumberOfDirectorsOrPartners {
  implicit val format: Format[NumberOfDirectorsOrPartners] = Json.format[NumberOfDirectorsOrPartners]
}

case class CorrespondenceCommonDetails(addressDetails:Address,
                                       contactDetails:Option[ContactDetails])

object CorrespondenceCommonDetails {
  implicit val format: Format[CorrespondenceCommonDetails] = Json.format[CorrespondenceCommonDetails]
}

case class PensionAdvisorDetails(name:String,
                                 addressDetails:Address,
                                 contactDetails:Option[ContactDetails])

object PensionAdvisorDetails {
  implicit val format: Format[PensionAdvisorDetails] = Json.format[PensionAdvisorDetails]
}
