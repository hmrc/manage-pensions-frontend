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

import play.api.libs.json.{Format, Json, OFormat}

case class Name(firstName: Option[String], middleName: Option[String], lastName: Option[String])

object Name {
  implicit val formats: OFormat[Name] = Json.format[Name]
}

case class PsaDetails(id: String, organisationOrPartnershipName: Option[String], individual: Option[Name])

object PsaDetails {
  implicit val formats: OFormat[PsaDetails] = Json.format[PsaDetails]
}

case class CorrespondenceAddress(addressLine1: String, addressLine2: String, addressLine3: Option[String],
                                 addressLine4: Option[String], countryCode: String, postalCode: Option[String])

object CorrespondenceAddress {
  implicit val formats: OFormat[CorrespondenceAddress] = Json.format[CorrespondenceAddress]
}

case class SchemeMemberNumbers(current: String, future: String)

object SchemeMemberNumbers {
  implicit val formats: OFormat[SchemeMemberNumbers] = Json.format[SchemeMemberNumbers]
}

case class InsuranceCompany(name: Option[String], policyNumber: Option[String], address: Option[CorrespondenceAddress])

object InsuranceCompany {
  implicit val formats: Format[InsuranceCompany] = Json.format[InsuranceCompany]
}

case class SchemeDetails(srn: Option[String],
                         pstr: Option[String],
                         status: String,
                         name: String,
                         isMasterTrust: Boolean,
                         typeOfScheme: Option[String],
                         otherTypeOfScheme: Option[String],
                         hasMoreThanTenTrustees: Boolean,
                         members: SchemeMemberNumbers,
                         isInvestmentRegulated: Boolean,
                         isOccupational: Boolean,
                         benefits: String,
                         country: String,
                         areBenefitsSecured: Boolean,
                         insuranceCompany: Option[InsuranceCompany])

object SchemeDetails {
  implicit val formats: Format[SchemeDetails] = Json.format[SchemeDetails]
}

case class PsaSchemeDetails(schemeDetails: SchemeDetails, psaDetails: Option[Seq[PsaDetails]])

object PsaSchemeDetails {
  implicit val formats: Format[PsaSchemeDetails] = Json.format[PsaSchemeDetails]
}
