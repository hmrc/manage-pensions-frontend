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

package views.behaviours

import forms.mappings.RegexBehaviourSpec
import play.api.data.Form
import play.api.data.FormError
import wolfendale.scalacheck.regexp.RegexpGen

trait StringFieldBehaviours extends FieldBehaviours with RegexBehaviourSpec {

  def fieldWithMaxLength(form: Form[_],
                         fieldName: String,
                         maxLength: Int,
                         lengthError: FormError): Unit = {

    s"not bind strings longer than $maxLength characters" in {

      forAll(stringsLongerThan(maxLength) -> "longString") {
        string =>
          val result = form.bind(Map(fieldName -> string)).apply(fieldName)
          result.errors shouldEqual Seq(lengthError)
      }
    }
  }

  def fieldWithRegex(form: Form[_],
                     fieldName: String,
                     invalidString: String,
                     error: FormError): Unit = {

    "not bind strings invalidated by regex" in {
      val result = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors shouldEqual Seq(error)
    }
  }

  def fieldWithTransform[A, B](form: Form[A],
                               transformName: String,
                               data: Map[String, String],
                               expected: B,
                               actual: A => B): Unit = {
    s"apply field transform $transformName" in {
      val result = form.bind(data)
      result.errors.size shouldBe 0
      actual(result.get) shouldBe expected
    }
  }

  def formWithTransform[A](form: Form[A],
                           data: Map[String, String],
                           expectedData: A): Unit = {
    s"bind the form with the transformation" in {
      val result = form.bind(data)
      result.errors.size shouldBe 0
      result.get shouldBe expectedData
    }
  }

  override def mandatoryField(form: Form[_],
                              fieldName: String,
                              requiredError: FormError): Unit = {

    "not bind spaces" in {
      forAll(RegexpGen.from("""^\s+$""")) { s =>
        val result = form.bind(Map(fieldName -> s)).apply(fieldName)
        result.errors shouldEqual Seq(requiredError)
      }
    }

    super.mandatoryField(form, fieldName, requiredError)
  }

  def optionalField[T](
                        form: Form[T],
                        fieldName: String,
                        validData: Map[String, String],
                        accessor: T => Option[String]
                      ): Unit = {

    "trim spaces" in {
      val value = validData(fieldName)
      forAll(RegexpGen.from("""^\s+""" + value + """\s+$""")) { s =>
        val result = form.bind(validData.updated(fieldName, s))
        accessor(result.get) shouldBe Some(value)
      }
    }

  }

}
