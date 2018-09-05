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

import play.api.libs.json.{Json, Format}

case class SubscriptionDetails(psaSubscriptionDetails:PsaSubscriptionDetailsType)

object SubscriptionDetails {
  implicit val format: Format[SubscriptionDetails] = Json.format[SubscriptionDetails]
}

case class PsaSubscriptionDetailsType(isPSASuspension:Boolean,
                                      directorOrPartnerDetails:Option[List[DirectorOrPartnerDetails]],
                                      previousAddressDetails:PreviousAddressDetailsType,
                                      correspondenceContactDetails:ContactDetailsType,
                                      numberOfDirectorsOrPartnersDetails:Option[NumberOfDirectorsOrPartnersType],
                                      organisationOrPartnerDetails:Option[OrganisationOrPartnerDetailsType],
                                      customerIdentificationDetails:CustomerIdentificationDetailsType,
                                      correspondenceAddressDetails:AddressType,
                                      individualDetails:Option[IndividualDetailsType],
                                      declarationDetails:PensionSchemeAdministratorDeclarationType)

object PsaSubscriptionDetailsType {
  implicit val format: Format[PsaSubscriptionDetailsType] = Json.format[PsaSubscriptionDetailsType]
}

case class CustomerIdentificationDetailsType(legalStatus:String,
                                             idType:Option[String],
                                             idNumber:Option[String],
                                             noIdentifier:Boolean)

object CustomerIdentificationDetailsType {
  implicit val format: Format[CustomerIdentificationDetailsType] = Json.format[CustomerIdentificationDetailsType]
}

case class OrganisationOrPartnerDetailsType(name:String,
                                            crnNumber:Option[String],
                                            vatRegistrationNumber:Option[String],
                                            payeReference:Option[String])

object OrganisationOrPartnerDetailsType {
  implicit val format: Format[OrganisationOrPartnerDetailsType] = Json.format[OrganisationOrPartnerDetailsType]
}


case class AddressType(nonUKAddress:Boolean,
                       line1:String,
                       line2:String,
                       line3:Option[String],
                       line4:Option[String],
                       postalCode:Option[String],
                       countryCode:String)

object AddressType {
  implicit val format: Format[AddressType] = Json.format[AddressType]
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
                                    previousAddressDetails:PreviousAddressDetailsType,
                                    correspondenceCommonDetails:Option[CorrespondenceCommonDetails])

object DirectorOrPartnerDetails {
  implicit val format: Format[DirectorOrPartnerDetails] = Json.format[DirectorOrPartnerDetails]
}

case class PensionSchemeAdministratorDeclarationType(box1:Boolean,
                                                     box2:Boolean,
                                                     box3:Boolean,
                                                     box4:Boolean,
                                                     box5:Option[Boolean],
                                                     box6:Option[Boolean],
                                                     box7:Boolean,
                                                     pensionAdvisorDetails:Option[PensionAdvisorDetails])

object PensionSchemeAdministratorDeclarationType {
  implicit val format: Format[PensionSchemeAdministratorDeclarationType] = Json.format[PensionSchemeAdministratorDeclarationType]
}

case class ContactDetailsType(telephone:String,
                              mobileNumber:Option[String],
                              fax:Option[String],
                              email:Option[String])

object ContactDetailsType {
  implicit val format: Format[ContactDetailsType] = Json.format[ContactDetailsType]
}

case class IndividualDetailsType(firstName:String,
                                 middleName:Option[String],
                                 dateOfBirth:String,
                                 lastName:String,
                                 title:Option[String])

object IndividualDetailsType {
  implicit val format: Format[IndividualDetailsType] = Json.format[IndividualDetailsType]
}

case class NumberOfDirectorsOrPartnersType(isMorethanTenDirectors:Option[Boolean],
                                           isMorethanTenPartners:Option[Boolean])


object NumberOfDirectorsOrPartnersType {
  implicit val format: Format[NumberOfDirectorsOrPartnersType] = Json.format[NumberOfDirectorsOrPartnersType]
}

case class CorrespondenceCommonDetails(addressDetails:AddressType,
                                       contactDetails:Option[ContactDetailsType])

object CorrespondenceCommonDetails {
  implicit val format: Format[CorrespondenceCommonDetails] = Json.format[CorrespondenceCommonDetails]
}

case class PreviousAddressDetailsType(isPreviousAddressLast12Month:Boolean,
                                      previousAddress:Option[AddressType])

object PreviousAddressDetailsType {
  implicit val format: Format[PreviousAddressDetailsType] = Json.format[PreviousAddressDetailsType]
}

case class PensionAdvisorDetails(name:String,
                                 addressDetails:AddressType,
                                 contactDetails:Option[ContactDetailsType])

object PensionAdvisorDetails {
  implicit val format: Format[PensionAdvisorDetails] = Json.format[PensionAdvisorDetails]
}