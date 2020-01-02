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

import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Forms, Mapping}

trait CheckboxMapping {

  def checkboxMapping(fieldName: String, trueValue: String, acceptTrueOnly: Boolean, invalidKey: String): Mapping[Boolean] = {
    Forms.optional(Forms.text)
      .verifying(checkboxConstraint(trueValue, acceptTrueOnly, invalidKey))
      .transform(transformFromCheckbox(trueValue), transformToCheckbox(trueValue))
  }

  private def checkboxConstraint(trueValue: String, acceptTrueOnly: Boolean, invalidKey: String): Constraint[Option[String]] =
    Constraint {
      case Some(value) if value == trueValue => Valid
      case None if !acceptTrueOnly => Valid
      case _ => Invalid(invalidKey)
    }

  private def transformFromCheckbox(trueValue: String)(value: Option[String]): Boolean = {
    value match {
      case Some(s) if s == trueValue => true
      case _ => false
    }
  }

  private def transformToCheckbox(trueValue: String)(value: Boolean): Option[String] = {
    if (value) {
      Some(trueValue)
    }
    else {
      None
    }
  }

}
