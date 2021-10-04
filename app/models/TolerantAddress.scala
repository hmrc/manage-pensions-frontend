/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.language.implicitConversions

case class TolerantAddress(addressLine1: Option[String],
                           addressLine2: Option[String],
                           addressLine3: Option[String],
                           addressLine4: Option[String],
                           postcode: Option[String],
                           countryOpt: Option[String]) {

  def lines: Seq[String] = {
    Seq(
      this.addressLine1,
      this.addressLine2,
      this.addressLine3,
      this.addressLine4,
      this.countryOpt,
      this.postcode
    ).flatten(s => s)
  }

  def print: String = {
    lines.mkString(", ")
  }

  private def prepopAddress: Address = {
    Address(
      addressLine1.getOrElse(""),
      addressLine2.getOrElse(""),
      addressLine3,
      addressLine4,
      postcode,
      countryOpt.getOrElse("")
    )
  }

  def toPrepopAddress: Address = toAddress.getOrElse(prepopAddress)


  def toAddress: Option[Address] = (addressLine1, addressLine2, countryOpt) match {
    case (Some(line1), Some(line2), Some(country)) => Some(Address(line1, line2, addressLine3, addressLine4, postcode, country))
    case (_, _, None) => None
    case (None, None, _) if addressLine3.nonEmpty && addressLine4.nonEmpty => shuffle
    case (Some(_), None, _) if addressLine3.nonEmpty || addressLine4.nonEmpty => shuffle
    case (None, Some(_), _) if addressLine3.nonEmpty || addressLine4.nonEmpty => shuffle
    case _ => None
  }

  private def shuffle: Option[Address] = (addressLine1, addressLine2, addressLine3, addressLine4) match {
    case (None, None, Some(line3), Some(line4)) => Some(Address(line3, line4, None, None, postcode, countryOpt.get))
    case (Some(line1), None, Some(line3), al4) => Some(Address(line1, line3, al4, None, postcode, countryOpt.get))
    case (Some(line1), None, None, Some(line4)) => Some(Address(line1, line4, None, None, postcode, countryOpt.get))
    case (None, Some(line2), Some(line3), al4) => Some(Address(line2, line3, al4, None, postcode, countryOpt.get))
    case (None, Some(line2), None, Some(line4)) => Some(Address(line2, line4, None, None, postcode, countryOpt.get))
    case _ => None
  }

  def equalsAddress(address: Address): Boolean = {
    address.addressLine1 == addressLine1.getOrElse("") &&
      address.addressLine2 == addressLine2.getOrElse("") &&
      address.addressLine3 == addressLine3 &&
      address.addressLine4 == addressLine4 &&
      address.country == countryOpt.getOrElse("") &&
      address.postcode == postcode
  }
}

object TolerantAddress {

  private val logger = Logger(classOf[TolerantAddress])

  val postCodeLookupAddressReads: Reads[TolerantAddress] = (
    (JsPath \ "address" \ "lines").read[List[String]] and
      (JsPath \ "address" \ "postcode").read[String] and
      (JsPath \ "address" \ "country" \ "code").read[String] and
      (JsPath \ "address" \ "town").readNullable[String] and
      (JsPath \ "address" \ "county").readNullable[String]
    ) ((lines, postCode, countryCode, town, county) => {

    val addressLines: (Option[String], Option[String], Option[String], Option[String]) = {
      lines.size match {
        case 0 =>
          logger.warn(s"[NoAddressLinesFoundException]-$postCode,$countryCode")
          (None, None, None, None)
        case 1 =>
          val townOrCounty = getTownOrCounty(town, county, lines)
          (Some(lines.head), townOrCounty._1, townOrCounty._2, None)
        case 2 =>
          val townOrCounty = getTownOrCounty(town, county, lines)
          (Some(lines.head), Some(lines(1)), townOrCounty._1, townOrCounty._2)
        case 3 =>
          val townOrCounty = getTownOrCounty(town, county, lines)
          val townOrCountyValue = if (townOrCounty._2.isDefined) townOrCounty._2 else townOrCounty._1
          (Some(lines.head), Some(lines(1)), Some(lines(2)), townOrCountyValue)
        case numberOfLines if numberOfLines >= 4 => (Some(lines.head), Some(lines(1)), Some(lines(2)), Some(lines(3)))
      }
    }
    TolerantAddress(addressLines._1, addressLines._2, addressLines._3, addressLines._4, Some(postCode), Some(countryCode))

  })

  private def checkIfElementAlreadyExistsInLines(lines: List[String], elementToCheck: String) = {
    lines.mkString("").toLowerCase().contains(elementToCheck.trim().toLowerCase())
  }

  //scalastyle:off cyclomatic.complexity
  private def getTownOrCounty(
                               town: Option[String],
                               county: Option[String],
                               addressLines: List[String]
                             ): (Option[String], Option[String]) = {

    (town, county) match {
      case (Some(town), None) =>
        val formattedTown = town
        (if (checkIfElementAlreadyExistsInLines(addressLines, formattedTown)) None else Some(formattedTown), None)
      case (None, Some(county)) =>
        val formattedCounty = county
        (if (checkIfElementAlreadyExistsInLines(addressLines, formattedCounty)) None else Some(formattedCounty), None)
      case (Some(town), Some(county)) =>
        val formattedTown = town
        val formattedCounty = county
        val townAlreadyExists = checkIfElementAlreadyExistsInLines(addressLines, formattedTown)
        val countyAlreadyExists = checkIfElementAlreadyExistsInLines(addressLines, formattedCounty)

        (townAlreadyExists, countyAlreadyExists) match {
          case (true, false) => (Some(county), None)
          case (false, true) => (Some(town), None)
          case (true, true) => (None, None)
          case _ => (Some(town), Some(county))
        }
      case _ => (None, None)
    }
  }

  val postCodeLookupReads: Reads[Seq[TolerantAddress]] = Reads {
    json =>
      json.validate[Seq[JsValue]].flatMap(addresses => {
        addresses.foldLeft[JsResult[List[TolerantAddress]]](JsSuccess(List.empty)) {
          (addresses, currentAddress) => {
            for {
              sequenceOfAddressess <- addresses
              address <- currentAddress.validate[TolerantAddress](postCodeLookupAddressReads)
            } yield sequenceOfAddressess :+ address
          }
        }
      })
  }

  implicit lazy val formatsTolerantAddress: Format[TolerantAddress] = (
    (JsPath \ "addressLine1").formatNullable[String] and
      (JsPath \ "addressLine2").formatNullable[String] and
      (JsPath \ "addressLine3").formatNullable[String] and
      (JsPath \ "addressLine4").formatNullable[String] and
      (JsPath \ "postalCode").formatNullable[String] and
      (JsPath \ "countryCode").formatNullable[String]
    ) (TolerantAddress.apply, unlift(TolerantAddress.unapply))

  implicit def convert(tolerant: TolerantAddress): Option[Address] = {
    for {
      addressLine1 <- tolerant.addressLine1
      addressLine2 <- tolerant.addressLine2
      country <- tolerant.countryOpt
    } yield {
      Address(
        addressLine1,
        addressLine2,
        tolerant.addressLine3,
        tolerant.addressLine4,
        tolerant.postcode,
        country
      )
    }
  }
}

