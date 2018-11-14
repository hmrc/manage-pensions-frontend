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

import org.joda.time.LocalDate

import play.api.data.validation.{Constraint, Invalid, Valid}
import utils.countryOptions.CountryOptions

import scala.language.implicitConversions

trait Constraints {

  import Constraints._

  protected def firstError[A](constraints: Constraint[A]*): Constraint[A] =
    Constraint {
      input =>
        constraints
          .map(_.apply(input))
          .find(_ != Valid)
          .getOrElse(Valid)
    }

  protected def minimumValue[A](minimum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input >= minimum) {
          Valid
        } else {
          Invalid(errorKey, minimum)
        }
    }

  protected def maximumValue[A](maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, maximum)
        }
    }

  protected def regexp(regex: String, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.matches(regex) =>
        Valid
      case _ =>
        Invalid(errorKey, regex)
    }

  protected def maxLength(maximum: Int, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length <= maximum =>
        Valid
      case _ =>
        Invalid(errorKey, maximum)
    }

  def returnOnFirstFailure[T](constraints: Constraint[T]*): Constraint[T] =
    Constraint {
      field =>
        constraints
          .map(_.apply(field))
          .filterNot(_ == Valid)
          .headOption.getOrElse(Valid)
    }

  implicit def convertToOptionalConstraint[T](constraint: Constraint[T]): Constraint[Option[T]] =
    Constraint {
      case Some(t) => constraint.apply(t)
      case _ => Valid
    }

  protected def adviserName(errorKey: String): Constraint[String] = regexp(adviserNameRegex, errorKey)

  protected def psaName(errorKey: String): Constraint[String] = regexp(psaNameRegex, errorKey)

  protected def psaId(errorKey: String): Constraint[String] = regexp(psaIdRegx, errorKey)

  protected def addressLine(errorKey: String): Constraint[String] = regexp(addressLineRegex, errorKey)

  protected def postCode(errorKey: String): Constraint[String] = regexp(postCodeRegex, errorKey)

  protected def email(errorKey: String): Constraint[String] = regexp(emailRegex, errorKey)

  protected def country(countryOptions: CountryOptions, errorKey: String): Constraint[String] =
    Constraint {
      input =>
        countryOptions.options
          .find(_.value == input)
          .map(_ => Valid)
          .getOrElse(Invalid(errorKey))
    }

  protected def nonFutureDate(errorKey: String): Constraint[LocalDate] =
    Constraint {
      case date if !LocalDate.now().isBefore(date) => Valid
      case _ => Invalid(errorKey)
    }

  protected def afterGivenDate(errorKey: String, givenDate: LocalDate): Constraint[LocalDate] =
    Constraint {
      case date if givenDate.isBefore(date) => Valid
      case _ => Invalid(errorKey)
    }
}

object Constraints {
  val psaIdRegx = """^A[0-9]{7}$"""
  val adviserNameRegex = """^[a-zA-Z\u00C0-\u00FF '‘’\u2014\u2013\u2010\u002d]{1,107}$"""
  val psaNameRegex = """^[a-zA-Z0-9-\u00C0-\u00FF '&\\/‘’\u2014\u2013\u2010\u002d]{1,107}$"""
  val addressLineRegex = """^[A-Za-z0-9 &!'‘’\"“”(),./\u2014\u2013\u2010\u002d]{1,35}$"""
  val postCodeRegex = """^[A-Za-z]{1,2}[0-9][0-9A-Za-z]?[ ]?[0-9][A-Za-z]{2}$"""
  val emailRegex = "^(?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"" +
    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")" +
    "@(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?|" +
    "\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-zA-Z0-9-]*[a-zA-Z0-9]:" +
    "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$"

}
