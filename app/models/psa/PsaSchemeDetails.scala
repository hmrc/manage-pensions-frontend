/*
 * Copyright 2024 HM Revenue & Customs
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

package models.psa

import models.SchemeStatus
import models.SchemeStatus.{Deregistered, Rejected, WoundUp}
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class Name(firstName: Option[String], middleName: Option[String], lastName: Option[String])

object Name {
  implicit val formats: OFormat[Name] = Json.format[Name]
}

case class PsaDetails(id: String, organisationOrPartnershipName: Option[String], individual: Option[Name], relationshipDate: Option[String]){
  def getPsaName: Option[String] = {
    (individual, organisationOrPartnershipName) match {
      case (Some(individual), _) => Some(fullName(individual))
      case (_, Some(org)) => Some(s"$org")
      case _ => None
    }
  }

  private def fullName(individual: Name): String =
    s"${individual.firstName.getOrElse("")} ${individual.middleName.getOrElse("")} ${individual.lastName.getOrElse("")}"

}

object PsaDetails {
  implicit val formats: OFormat[PsaDetails] = Json.format[PsaDetails]
}

case class PsaSchemeDetails(psaDetails: Option[Seq[PsaDetails]])

object PsaSchemeDetails {

  implicit val formats: OFormat[PsaSchemeDetails] = Json.format[PsaSchemeDetails]

  private val statusesWhereSoleOwnerCanBeRemoved = Set[SchemeStatus](WoundUp, Rejected, Deregistered)

  def canRemovePsaVariations(psaId: String, schemeAdmins:Seq[PsaDetails], schemeStatus:String): Boolean = {
    val status = SchemeStatus.forValue(schemeStatus)
    status.canRemovePsa && hasMinimumAttachedPSAs(psaId, schemeAdmins, status) && psaNotRemovingOnSameDay(psaId, schemeAdmins)
  }

  private def hasMinimumAttachedPSAs(psaId: String, psaDetails: Seq[PsaDetails], status: SchemeStatus): Boolean =
    statusesWhereSoleOwnerCanBeRemoved.contains(status) || psaDetails.exists(_.id != psaId)

  private def psaNotRemovingOnSameDay(psaId: String, psaDetails: Seq[PsaDetails]): Boolean = {
    !psaDetails.exists(details => details.id == psaId && details.relationshipDate.exists(date => LocalDate.parse(date).isEqual(LocalDate.now())))
  }
}
