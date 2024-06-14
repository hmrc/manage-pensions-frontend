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

import play.api.i18n.Messages
import play.api.libs.json.{JsString, Writes}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.{DateHelper, Enumerable}

import java.time.LocalDate

case class TaxYear(startYear: String) {
  def endYear: String = (startYear.toInt + 1).toString

  def rangeAsSeqString: (String, String) = (startYear, endYear)
}

object TaxYear extends Enumerable.Implicits {
  implicit val writes: Writes[TaxYear] = (yr: TaxYear) => JsString(yr.startYear)
  private val numberOfYearsToShow = 7

  def values: Seq[TaxYear] = {
    yearRange(DateHelper.today).reverse
  }


  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages("chooseTaxYear.yearRangeRadio", value.startYear, value.endYear)),
        value = Some(value.startYear),
        id = Some(s"value_$index")
      )
  }

  def optionsFiltered(func: TaxYear => Boolean)(implicit messages: Messages): Seq[RadioItem] = {
    values.zipWithIndex.flatMap {
      case (value, index) =>
        if (func(value)) {
          Seq(RadioItem(
            content = Text(messages("chooseTaxYear.yearRangeRadio", value.startYear, value.endYear)),
            value = Some(value.startYear),
            id = Some(s"value_$index")
          )
          )
        } else {
          Nil
        }
    }
  }

  def optionsFilteredTaxYear(func: TaxYear => Boolean): Seq[TaxYear] = {
    values.zipWithIndex.flatMap {
      case (value, index) =>
        if (func(value)) {
          Seq(TaxYear(
            value.startYear
          )
          )
        } else {
          Nil
        }
    }
  }

  def yearRange(currentDate: LocalDate): Seq[TaxYear] = {
    val endOfTaxYear = LocalDate.of(currentDate.getYear, 4, 5)
    val startOfTaxYear = LocalDate.of(currentDate.getYear, 4, 6)

    val currentTaxYearCalculated = if (currentDate.isBefore(startOfTaxYear)) {
      endOfTaxYear.getYear - 1
    }
    else {
      startOfTaxYear.getYear
    }
    (currentTaxYearCalculated - (numberOfYearsToShow - 1) to currentTaxYearCalculated).map(year => TaxYear(year.toString))
  }


  implicit val enumerable: Enumerable[TaxYear] =
    Enumerable(values.map(v => v.startYear -> v): _*)
}
