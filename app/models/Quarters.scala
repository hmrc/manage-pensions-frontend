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

import java.time.LocalDate

import config.FrontendAppConfig
import play.api.libs.json.{Format, Json}
import utils.DateHelper.{currentDate, _}

import scala.language.implicitConversions

sealed trait Quarters {
  def startDay: Int = 1
  def endDay: Int = 31
  def startMonth: Int
  def endMonth: Int
}
object Quarters {

  private val currentYear = currentDate.getYear

  case object Q1 extends WithName("q1") with Quarters {
    override def startMonth = 1
    override def endMonth = 3
  }

  case object Q2 extends WithName("q2") with Quarters {
    override def endDay = 30
    override def startMonth = 4
    override def endMonth = 6
  }

  case object Q3 extends WithName("q3") with Quarters {
    override def endDay = 30
    override def startMonth = 7
    override def endMonth = 9
  }

  case object Q4 extends WithName("q4") with Quarters {
    override def startMonth = 10
    override def endMonth = 12
  }

  def getQuarterDates(quarter: Quarters, year: Int): QuarterDates = {
    QuarterDates(LocalDate.of(year, quarter.startMonth, quarter.startDay),
      LocalDate.of(year, quarter.endMonth, quarter.endDay))
  }

  def getQuarterDates(date: LocalDate): QuarterDates = {
    getQuarterDates(getQuartersFromDate(date), date.getYear)
  }

  def getCurrentQuarter: QuarterDates =
    getQuarterDates(getQuartersFromDate(currentDate), currentDate.getYear)

  def getQuartersFromDate(date: LocalDate): Quarters =
    date.getMonthValue match {
      case i if i <= 3 => Q1
      case i if i <= 6 => Q2
      case i if i <= 9 => Q3
      case _ => Q4
    }

  def validQuartersForYear(year: Int)(implicit config: FrontendAppConfig): Seq[Quarters] = {
    val earliestValidYear: Int = LocalDate.parse(config.quarterStartDate).getYear
    year match {
      case _ if year == currentYear => getCurrentYearQuarters
      case _ if year == earliestValidYear => Seq(Q2, Q3, Q4)
      case _ => Seq(Q1, Q2, Q3, Q4)
    }
  }

  private def getCurrentYearQuarters(implicit config: FrontendAppConfig): Seq[Quarters] = {
    val earliestValidYear: Int = LocalDate.parse(config.quarterStartDate).getYear
    val quartersCY = currentDate.getMonthValue match {
      case i if i > 9 => Seq(Q1, Q2, Q3, Q4)
      case i if i > 6 => Seq(Q1, Q2, Q3)
      case i if i  > 3 => Seq(Q1, Q2)
      case _ => Seq(Q1)
    }

    if(currentYear == earliestValidYear) {
      quartersCY.filter(_ != Q1)
    }
    else {
      quartersCY
    }
  }

}

case class QuarterDates(startDate: LocalDate, endDate: LocalDate)

object QuarterDates {
  implicit lazy val formats: Format[QuarterDates] =
    Json.format[QuarterDates]
}