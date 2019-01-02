/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.libs.json.{Json, OFormat}

case class Name(firstName: Option[String], middleName: Option[String], lastName: Option[String])

object Name {
  implicit val formats: OFormat[Name] = Json.format[Name]
}

case class PsaDetails(id: String, organisationOrPartnershipName: Option[String], individual: Option[Name], relationshipDate: Option[String])

object PsaDetails {
  def getPsaName(psaDetails: PsaDetails): Option[String] = {
    (psaDetails.individual, psaDetails.organisationOrPartnershipName) match {
      case (Some(individual), _) => Some(fullName(individual))
      case (_, Some(org)) => Some(s"$org")
      case _ => None
    }
  }

  private def fullName(individual: Name): String =
    s"${individual.firstName.getOrElse("")} ${individual.middleName.getOrElse("")} ${individual.lastName.getOrElse("")}"

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
  implicit val formats: OFormat[InsuranceCompany] = Json.format[InsuranceCompany]
}

case class PreviousAddressInfo(isPreviousAddressLast12Month: Boolean,
                               previousAddress: Option[CorrespondenceAddress] = None)

object PreviousAddressInfo {
  implicit val formats: OFormat[PreviousAddressInfo] = Json.format[PreviousAddressInfo]
}

case class PersonalInfo(name: IndividualName, dateOfBirth: String)

object PersonalInfo {
  implicit val formats: OFormat[PersonalInfo] = Json.format[PersonalInfo]
}

case class IndividualName(firstName: String, middleName: Option[String], lastName: String)

object IndividualName {
  implicit val formats: OFormat[IndividualName] = Json.format[IndividualName]
}

case class IndividualInfo(personalDetails: PersonalInfo,
                          nino: Option[String],
                          utr: Option[String],
                          address: CorrespondenceAddress,
                          contact: IndividualContactDetails,
                          previousAddress: PreviousAddressInfo)

object IndividualInfo {
  implicit val formats: OFormat[IndividualInfo] = Json.format[IndividualInfo]
}

case class IndividualContactDetails(telephone: String, email: String)

object IndividualContactDetails {

  implicit val formats: OFormat[IndividualContactDetails] = Json.format[IndividualContactDetails]
}


case class PartnershipDetails(partnershipName: String,
                              utr: Option[String],
                              vatRegistration: Option[String],
                              payeRef: Option[String],
                              address: CorrespondenceAddress,
                              contact: IndividualContactDetails,
                              previousAddress: PreviousAddressInfo,
                              partnerDetails: Seq[IndividualInfo])

object PartnershipDetails {
  implicit val formats: OFormat[PartnershipDetails] = Json.format[PartnershipDetails]
}

case class CompanyDetails(organizationName: String,
                          utr: Option[String],
                          crn: Option[String],
                          vatRegistration: Option[String],
                          payeRef: Option[String],
                          address: CorrespondenceAddress,
                          contact: IndividualContactDetails,
                          previousAddress: Option[PreviousAddressInfo],
                          directorsDetails: Seq[IndividualInfo])


object CompanyDetails {
  implicit val formats: OFormat[CompanyDetails] = Json.format[CompanyDetails]
}

case class EstablisherInfo(individual: Seq[IndividualInfo],
                           company: Seq[CompanyDetails],
                           partnership: Seq[PartnershipDetails])

object EstablisherInfo {
  implicit val formats: OFormat[EstablisherInfo] = Json.format[EstablisherInfo]
}

case class TrusteeInfo(individual: Seq[IndividualInfo],
                       company: Seq[CompanyDetails],
                       partnership: Seq[PartnershipDetails])

object TrusteeInfo {
  implicit val formats: OFormat[TrusteeInfo] = Json.format[TrusteeInfo]
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
  implicit val formats: OFormat[SchemeDetails] = Json.format[SchemeDetails]
}

case class PsaSchemeDetails(schemeDetails: SchemeDetails,
                            establisherDetails: Option[EstablisherInfo],
                            trusteeDetails: Option[TrusteeInfo],
                            psaDetails: Option[Seq[PsaDetails]])

object PsaSchemeDetails {

  implicit val formats: OFormat[PsaSchemeDetails] = Json.format[PsaSchemeDetails]

  def canRemovePsa(psaId: String, scheme: PsaSchemeDetails): Boolean = {

    SchemeStatus.forValue(scheme.schemeDetails.status).canRemovePsa &&
      scheme.psaDetails.exists(psaDetails =>
        isOtherPSAsExist(psaId, psaDetails) && psaNotRemovingOnSameDay(psaId, psaDetails)
      )
  }

  private def isOtherPSAsExist(psaId: String, psaDetails: Seq[PsaDetails]): Boolean = psaDetails.exists(_.id != psaId)

  private def psaNotRemovingOnSameDay(psaId: String, psaDetails: Seq[PsaDetails]): Boolean = {
    !psaDetails.exists(details => details.id == psaId && details.relationshipDate.exists(new LocalDate(_).isEqual(LocalDate.now())))
  }
}
