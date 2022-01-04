/*
 * Copyright 2022 HM Revenue & Customs
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


case class IndividualDetails(firstName: String, middleName: Option[String], lastName: String) {
  def fullName: String = middleName match {
    case Some(middle) => s"$firstName $middle $lastName"
    case None => s"$firstName $lastName"
  }
}

object IndividualDetails {
  implicit val format: Format[IndividualDetails] = Json.format[IndividualDetails]
}

case class MinimalPSAPSP(
                          email: String,
                          isPsaSuspended: Boolean,
                          organisationName: Option[String],
                          individualDetails: Option[IndividualDetails],
                          rlsFlag: Boolean,
                          deceasedFlag: Boolean
                        ) {
  def name: String = {
    individualDetails
      .map(_.fullName)
      .orElse(organisationName)
      .getOrElse("Pension Scheme Administrator")
  }
}

object MinimalPSAPSP {
  implicit val format: Format[MinimalPSAPSP] = Json.format[MinimalPSAPSP]

  def getNameFromId(minDetails: MinimalPSAPSP): Option[String] =
    (minDetails.individualDetails, minDetails.organisationName) match {
      case (Some(individual), None) => Some(individual.fullName)
      case (None, Some(org)) => Some(s"$org")
      case _ => None
    }
}
