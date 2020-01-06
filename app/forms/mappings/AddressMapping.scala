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

import play.api.data.{FormError, Mapping}
import utils.countryOptions.CountryOptions

trait AddressMapping extends Mappings with Transforms with Constraints {

  import AddressMapping._

  def addressLineMapping(keyRequired: String, keyLength: String, keyInvalid: String): Mapping[String] = {
    text(keyRequired)
      .verifying(
        firstError(
          maxLength(
            maxAddressLineLength,
            keyLength
          ),
          addressLine(keyInvalid)
        )
      )
  }

  def optionalAddressLineMapping(keyLength: String, keyInvalid: String): Mapping[Option[String]] = {
    optionalText()
      .verifying(
        firstError(
          maxLength(
            maxAddressLineLength,
            keyLength
          ),
          addressLine(keyInvalid)
        )
      )
  }

  private[mappings] def postCodeTransform(value: String): String = {
    minimiseSpace(value.trim.toUpperCase)
  }

  private[mappings] def postCodeValidTransform(value: String): String = {
    value match {
      case postCodeRegex(_*) if !value.contains(" ") =>
        value.substring(0, value.length - 3) + " " + value.substring(value.length - 3, value.length)
      case _ => value
    }
  }

  def postCodeMapping(keyRequired: String, keyLength: String, keyInvalid: String): Mapping[String] = {
    text(keyRequired)
      .transform(postCodeTransform, noTransform)
      .verifying(
        firstError(
          maxLength(
            maxPostCodeLength,
            keyLength
          ),
          postCode(keyInvalid)
        )
      )
      .transform(postCodeValidTransform, noTransform)
  }

  private def postCodeDataTransform(value: Option[String]): Option[String] = {
    value.map(postCodeTransform).filter(_.nonEmpty)
  }

  private def countryDataTransform(value: Option[String]): Option[String] = {
    value.map(s => strip(s).toUpperCase()).filter(_.nonEmpty)
  }

  def postCodeWithCountryMapping(keyRequired: String, keyInvalid: String, keyNonUKLength: String): Mapping[Option[String]] = {

    val fieldName = "postCode"

    def bind(data: Map[String, String]): Either[Seq[FormError], Option[String]] = {
      val postCode = postCodeDataTransform(data.get(fieldName))
      val country = countryDataTransform(data.get("country"))
      val maxLengthNonUKPostCode = 10

      (postCode, country) match {
        case (Some(zip@postCodeRegex(_*)), Some("GB")) => Right(Some(postCodeValidTransform(zip)))
        case (Some(_), Some("GB")) => Left(Seq(FormError(fieldName, keyInvalid)))
        case (Some(zip), Some(_)) if zip.length <= maxLengthNonUKPostCode => Right(Some(zip))
        case (Some(_), Some(_)) => Left(Seq(FormError(fieldName, keyNonUKLength)))
        case (Some(zip), None) => Right(Some(zip))
        case (None, Some("GB")) => Left(Seq(FormError(fieldName, keyRequired)))
        case _ => Right(None)
      }
    }

    def unbind(value: Option[String]): Map[String, String] = {
      Map(fieldName -> value.getOrElse(""))
    }

    new CustomBindMapping(fieldName, bind, unbind)

  }

  def countryMapping(countryOptions: CountryOptions, keyRequired: String, keyInvalid: String): Mapping[String] = {
    text(keyRequired)
      .verifying(country(countryOptions, keyInvalid))
  }
}

object AddressMapping {
  val maxAddressLineLength = 35
  val maxPostCodeLength = 8
  val postCodeRegex = """^[A-Za-z]{1,2}[0-9][0-9A-Za-z]?[ ]?[0-9][A-Za-z]{2}$""".r
}
