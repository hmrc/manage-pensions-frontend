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

case class Address(nonUKAddress: Boolean, line1: String, line2: String, line3: Option[String], line4: Option[String],
                   postalCode: Option[String], countryCode: String)

object Address {
  implicit val formats: Format[Address] = Json.format[Address]
}

case class ContactDetails(telephone: String, mobileNumber: Option[String], fax: Option[String], email: String)

object ContactDetails {
  implicit val formats: Format[ContactDetails] = Json.format[ContactDetails]
}

case class SchemeDetails(srn: String, pstr: String, schemeStatus: String, schemeName: String, isSchemeMasterTrust: Option[Boolean],
                         pensionSchemeStructure: Option[String], otherPensionSchemeStructure: Option[String],
                         hasMoreThanTenTrustees: Option[Boolean], currentSchemeMembers: String, futureSchemeMembers: String,
                         isReguledSchemeInvestment: Boolean, isOccupationalPensionScheme: Boolean, schemeProvideBenefits: String,
                         schemeEstablishedCountry: String, invalidBankFlag: Boolean, isSchemeBenefitsInsuranceCompany: Boolean,
                         insuranceCompanyName: Option[String], policyNumber: Option[String], insuranceCompanyAddressDetails: Option[Address],
                         insuranceCompanyContactDetails: Option[ContactDetails]
                        )

object SchemeDetails{
  implicit val formats: Format[SchemeDetails] = Json.format[SchemeDetails]
}

case class PersonDetails(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                         lastName: String, dateOfBirth: String)

object PersonDetails {
  implicit val formats: Format[PersonDetails] = Json.format[PersonDetails]
}

case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean, previousAddress: Option[Address])

object PreviousAddressDetails {
  implicit val formats: Format[PreviousAddressDetails] = Json.format[PreviousAddressDetails]
}

case class Individual(
                       personDetails: PersonDetails,
                       nino: Option[String] = None,
                       noNinoReason: Option[String] = None,
                       utr: Option[String] = None,
                       noUtrReason: Option[String] = None,
                       correspondenceAddressDetails: Address,
                       correspondenceContactDetails: ContactDetails,
                       previousAddressDetails: PreviousAddressDetails
                     )

object Individual{
  implicit val formats: Format[Individual] = Json.format[Individual]
}

case class CompanyEstablisher(
                               organisationName: String,
                               utr: Option[String] = None,
                               noUtrReason: Option[String] = None,
                               crnNumber: Option[String] = None,
                               noCrnReason: Option[String] = None,
                               vatRegistrationNumber: Option[String] = None,
                               payeReference: Option[String] = None,
                               haveMoreThanTenDirectors: Option[Boolean],
                               correspondenceAddressDetails: Address,
                               correspondenceContactDetails: ContactDetails,
                               previousAddressDetails: Option[PreviousAddressDetails],
                               directorDetails: Option[Seq[Individual]]
                             )

object CompanyEstablisher{
  implicit val formats: Format[CompanyEstablisher] = Json.format[CompanyEstablisher]
}

case class Partnership(
                        partnershipName: String,
                        utr: Option[String] = None,
                        noUtrReason: Option[String] = None,
                        vatRegistrationNumber: Option[String] = None,
                        payeReference: Option[String] = None,
                        areMorethanTenPartners: Boolean,
                        correspondenceAddressDetails: Address,
                        correspondenceContactDetails: ContactDetails,
                        previousAddressDetails: PreviousAddressDetails,
                        partnerDetails: Seq[Individual]
                      )

object Partnership{
  implicit val formats: Format[Partnership] = Json.format[Partnership]
}


case class EstablisherDetails(
                               individualDetails: Option[Seq[Individual]],
                               companyOrOrganisationDetails: Option[Seq[CompanyEstablisher]],
                               partnershipTrusteeDetail: Option[Seq[Partnership]]
                             )

object EstablisherDetails{
  implicit val formats: Format[EstablisherDetails] = Json.format[EstablisherDetails]
}

case class CompanyTrustee(
                           organizationName: String,
                           utr: Option[String] = None,
                           noUtrReason: Option[String] = None,
                           crnNumber: Option[String] = None,
                           noCrnReason: Option[String] = None,
                           vatRegistrationNumber: Option[String] = None,
                           payeReference: Option[String] = None,
                           correspondenceAddressDetails: Address,
                           correspondenceContactDetails: ContactDetails,
                           previousAddressDetails: PreviousAddressDetails
                         )

object CompanyTrustee{
  implicit val formats: Format[CompanyTrustee] = Json.format[CompanyTrustee]
}

case class PartnershipTrustee(
                               partnershipName: String,
                               utr: Option[String] = None,
                               noUtrReason: Option[String] = None,
                               vatRegistrationNumber: Option[String] = None,
                               payeReference: Option[String] = None,
                               correspondenceAddressDetails: Address,
                               correspondenceContactDetails: ContactDetails,
                               previousAddressDetails: PreviousAddressDetails
                             )

object PartnershipTrustee{
  implicit val formats: Format[PartnershipTrustee] = Json.format[PartnershipTrustee]
}

case class TrusteeDetails(
                           individualTrusteeDetail: Seq[Individual],
                           companyTrusteeDetail: Seq[CompanyTrustee],
                           partnershipTrusteeDetail: Seq[PartnershipTrustee]
                         )

object TrusteeDetails{
  implicit val formats: Format[TrusteeDetails] = Json.format[TrusteeDetails]
}

case class PsaDetails(
                       psaid: String,
                       organizationOrPartnershipName: Option[String] = None,
                       firstName: Option[String] = None,
                       middleName: Option[String] = None,
                       lastName: Option[String] = None,
                       relationshipType: Option[String] = None,
                       relationshipDate: Option[String] = None
                     )

object PsaDetails{
  implicit val formats: Format[PsaDetails] = Json.format[PsaDetails]
}

case class PensionsScheme(schemeDetails: SchemeDetails, establisherDetails: Option[EstablisherDetails],
                          trusteeDetails: Option[TrusteeDetails], psaDetails: Option[Seq[PsaDetails]])

object PensionsScheme {
  implicit val formats: Format[PensionsScheme] = Json.format[PensionsScheme]
}

case class PsaSchemeDetails(psaSchemeDetails: PensionsScheme)

object PsaSchemeDetails {
  implicit val formats: Format[PsaSchemeDetails] = Json.format[PsaSchemeDetails]
}