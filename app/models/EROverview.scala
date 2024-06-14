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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class EROverviewVersion(
                              numberOfVersions: Int,
                              submittedVersionAvailable: Boolean,
                              compiledVersionAvailable: Boolean
                            )

object EROverviewVersion {
  implicit val rds: Reads[Option[EROverviewVersion]] = {
    (JsPath \ "tpssReportPresent").readNullable[Boolean].flatMap {
      case Some(true) => Reads(_ => JsSuccess(None))
      case _ => (
        (JsPath \ "versionDetails" \ "numberOfVersions").read[Int] and
          (JsPath \ "versionDetails" \ "submittedVersionAvailable").read[Boolean] and
          (JsPath \ "versionDetails" \ "compiledVersionAvailable").read[Boolean]
        ) (
        (noOfVersions, isSubmitted, isCompiled) =>
          Some(EROverviewVersion(
            noOfVersions,
            isSubmitted,
            isCompiled
          )))
    }
  }
    implicit val formats: Format[EROverviewVersion] = Json.format[EROverviewVersion]
}

case class EROverview(
                       periodStartDate: LocalDate,
                       periodEndDate: LocalDate,
                       taxYear: TaxYear,
                       tpssReportPresent: Boolean,
                       versionDetails: Option[EROverviewVersion]
                     )

object EROverview {
  implicit val rds: Reads[EROverview] = (
    (JsPath \ "periodStartDate").read[String] and
      (JsPath \ "periodEndDate").read[String] and
      (JsPath \ "tpssReportPresent").readNullable[Boolean].flatMap {
        case Some(true) => Reads(_ => JsSuccess(true))
        case _ => Reads(_ => JsSuccess(false))
      } and EROverviewVersion.rds
    ) (
    (startDate, endDate, tpssReport, versionDetails) =>
      EROverview(
        LocalDate.parse(startDate),
        LocalDate.parse(endDate),
        TaxYear( LocalDate.parse(startDate).getYear.toString ),
        tpssReport,
        versionDetails))

  implicit val formats: Format[EROverview] = Json.format[EROverview]
}
