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

import models.SchemeStatus.Deregistered
import models.SchemeStatus.Rejected
import models.SchemeStatus.WoundUp
import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.libs.json.OFormat

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
    !psaDetails.exists(details => details.id == psaId && details.relationshipDate.exists(new LocalDate(_).isEqual(LocalDate.now())))
  }
}
