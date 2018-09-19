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

package forms.mappings

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.{Matchers, WordSpec}
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}

trait RegexBehaviourSpec extends TableDrivenPropertyChecks {

  this: WordSpec with Matchers =>

  def regexWithValidAndInvalidExamples(
                                        constraint: String => Constraint[String],
                                        valid: TableFor1[String], invalid: TableFor1[String],
                                        invalidMsg: String,
                                        regexString: String): Unit = {
    "Accept all valid examples" in {
      forAll(valid) { value: String =>
        constraint(invalidMsg)(value) shouldBe Valid
      }
    }

    "Reject all invalid examples" in {
      forAll(invalid) { value: String =>
        constraint(invalidMsg)(value) shouldBe Invalid(invalidMsg, regexString)
      }
    }
  }

  def formWithRegex(
                     form: Form[_],
                     valid: TableFor1[Map[String, String]],
                     invalid: TableFor1[Map[String, String]]
                   ): Unit = {

    "Accept all valid examples" in {
      forAll(valid) { data =>
        form.bind(data).errors.isEmpty shouldBe true
      }
    }

    "Reject all invalid examples" in {
      forAll(invalid) { data =>
        form.bind(data).errors.nonEmpty shouldBe true
      }
    }
  }

}
