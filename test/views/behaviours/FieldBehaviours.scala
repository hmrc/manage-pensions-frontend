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

package views.behaviours

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import forms.FormSpec
import generators.Generators
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import play.api.data.{Form, FormError}

trait FieldBehaviours extends FormSpec with PropertyChecks with Generators {

  def fieldThatBindsValidData(form: Form[_],
                              fieldName: String,
                              validDataGenerator: Gen[String]): Unit = {

    "bind valid data" in {

      forAll(validDataGenerator.retryUntil(!_.matches("""^\s+$""")) -> "validDataItem") {
        dataItem: String =>
          val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
          result.errors shouldBe empty
      }
    }
  }

  def mandatoryField(form: Form[_],
                     fieldName: String,
                     requiredError: FormError): Unit = {

    "not bind when key is not present at all" in {

      val result = form.bind(emptyForm).apply(fieldName)
      result.errors shouldEqual Seq(requiredError)
    }

    "not bind blank values" in {

      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors shouldEqual Seq(requiredError)
    }
  }

  def dateFieldThatBindsValidData(form: Form[_], fieldName: String, generator: Gen[String]): Unit = {
    "bind valid dates to day/month/year" in {
      val dayFieldName = s"$fieldName.day"
      val monthFieldName = s"$fieldName.month"
      val yearFieldName = s"$fieldName.year"

      val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

      def testField(fieldName: String, data: String): Unit = {
        form.bind(Map(fieldName -> data)).apply(fieldName).errors shouldBe empty
      }

      forAll(generator -> "date") {
        dateAsText: String =>
          val date = LocalDate.parse(dateAsText, formatter)
          testField(dayFieldName, date.getDayOfMonth.toString)
          testField(monthFieldName, date.getMonthValue.toString)
          testField(yearFieldName, date.getYear.toString)
      }
    }
  }

  def mandatoryDateField(form: Form[_], fieldName: String, requiredError: FormError): Unit = {
    val dayFieldName = s"$fieldName.day"
    val monthFieldName = s"$fieldName.month"
    val yearFieldName = s"$fieldName.year"

    def keyNotPresent(fieldName: String, requiredKey: String): Unit = {
      val result = form.bind(emptyForm).apply(fieldName)
      result.errors shouldEqual Seq(FormError(fieldName, requiredKey))
    }

    def keyBlank(fieldName: String, requiredKey: String): Unit = {
      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors shouldEqual Seq(FormError(fieldName, requiredKey))
    }

    "not bind when day key is not present at all" in {
      keyNotPresent(dayFieldName, "error.date.day_blank")
    }

    "not bind when month key is not present at all" in {
      keyNotPresent(monthFieldName, "error.date.month_blank")
    }

    "not bind when year key is not present at all" in {
      keyNotPresent(yearFieldName, "error.date.year_blank")
    }

    "not bind when day key is blank" in {
      keyBlank(dayFieldName, "error.date.day_blank")
    }

    "not bind when month key is blank" in {
      keyBlank(monthFieldName, "error.date.month_blank")
    }

    "not bind when year key is blank" in {
      keyBlank(yearFieldName, "error.date.year_blank")
    }
  }

}
