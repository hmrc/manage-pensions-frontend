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

package forms.mappings

import java.time.LocalDate
import org.scalatest.{Matchers, WordSpec}
import play.api.data.validation.{Invalid, Valid}
import utils.InputOption
import wolfendale.scalacheck.regexp.RegexpGen

// scalastyle:off magic.number

class ConstraintsSpec extends WordSpec with Matchers with Constraints with RegexBehaviourSpec {

  import Constraints._

  "firstError" must {

    "return Valid when all constraints pass" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("foo")
      result shouldEqual Valid
    }

    "return Invalid when the first constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("a" * 11)
      result shouldEqual Invalid("error.length", 10)
    }

    "return Invalid when the second constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result shouldEqual Invalid("error.regexp", """^\w+$""")
    }

    "return Invalid for the first error when both constraints fail" in {
      val result = firstError(maxLength(-1, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result shouldEqual Invalid("error.length", -1)
    }
  }

  "minimumValue" must {

    "return Valid for a number greater than the threshold" in {
      val result = minimumValue(1, "error.min").apply(2)
      result shouldEqual Valid
    }

    "return Valid for a number equal to the threshold" in {
      val result = minimumValue(1, "error.min").apply(1)
      result shouldEqual Valid
    }

    "return Invalid for a number below the threshold" in {
      val result = minimumValue(1, "error.min").apply(0)
      result shouldEqual Invalid("error.min", 1)
    }
  }

  "maximumValue" must {

    "return Valid for a number less than the threshold" in {
      val result = maximumValue(1, "error.max").apply(0)
      result shouldEqual Valid
    }

    "return Valid for a number equal to the threshold" in {
      val result = maximumValue(1, "error.max").apply(1)
      result shouldEqual Valid
    }

    "return Invalid for a number above the threshold" in {
      val result = maximumValue(1, "error.max").apply(2)
      result shouldEqual Invalid("error.max", 1)
    }
  }

  "regexp" must {

    "return Valid for an input that matches the expression" in {
      val result = regexp("""^\w+$""", "error.invalid")("foo")
      result shouldEqual Valid
    }

    "return Invalid for an input that does not match the expression" in {
      val result = regexp("""^\d+$""", "error.invalid")("foo")
      result shouldEqual Invalid("error.invalid", """^\d+$""")
    }
  }

  "maxLength" must {

    "return Valid for a string shorter than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 9)
      result shouldEqual Valid
    }

    "return Valid for an empty string" in {
      val result = maxLength(10, "error.length")("")
      result shouldEqual Valid
    }

    "return Valid for a string equal to the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 10)
      result shouldEqual Valid
    }

    "return Invalid for a string longer than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 11)
      result shouldEqual Invalid("error.length", 10)
    }
  }

  "nonFutureDate" must {
    "return Valid for a date in the past" in {
      val result = nonFutureDate("error.invalidDate")(LocalDate.now().minusDays(1))
      result shouldEqual Valid
    }

    "return Valid for current date" in {
      val result = nonFutureDate("error.invalidDate")(LocalDate.now())
      result shouldEqual Valid
    }

    "return Invalid for a date in the future" in {
      val result = nonFutureDate("error.invalidDate")(LocalDate.now().plusDays(1))
      result shouldEqual Invalid("error.invalidDate")
    }
  }

  "afterGivenDate" must {
    "return Valid if the date is after given date" in {
      val result = afterGivenDate("error.invalidDate", LocalDate.now())(LocalDate.now().plusDays(1))
      result shouldEqual Valid
    }

    "return Valid if the date is on the given date" in {
      val result = afterGivenDate("error.invalidDate", LocalDate.now())(LocalDate.now())
      result shouldEqual Valid
    }

    "return Invalid if the date is before given date" in {
      val result = afterGivenDate("error.invalidDate", LocalDate.now())(LocalDate.now().minusDays(1))
      result shouldEqual Invalid("error.invalidDate")
    }
  }

  "psaName" must {

    val validText = RegexpGen.from(Constraints.psaNameRegex)

    val invalidText = Table(
      "test<name",
      "1234>",
      "",
      "{test name}"
    )

    val invalidMsg = "Invalid text"

    behave like regexWithValidAndInvalidExamples(psaName, validText, invalidText, invalidMsg, Constraints.psaNameRegex)
  }

  "PSAId" must {

    val validText = RegexpGen.from(psaIdRegx)

    val invalidText = Table(
      "A0000",
      "B0000000",
      "A12345678",
      "Ae000000"
    )

    val invalidMsg = "Invalid PSA Id"

    behave like regexWithValidAndInvalidExamples(psaId, validText, invalidText, invalidMsg, psaIdRegx)
  }

}
