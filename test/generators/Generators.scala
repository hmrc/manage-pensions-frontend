/*
 * Copyright 2019 HM Revenue & Customs
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

package generators

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.util.Random

// scalastyle:off magic.number
trait Generators {

  implicit val dontShrink: Shrink[String] = Shrink.shrinkAny

  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def intsInRangeWithCommas(min: Int, max: Int): Gen[String] = {
    val numberGen = choose[Int](min, max)
    val formatter = NumberFormat.getIntegerInstance
    val random = Random

    numberGen.map(n => {
      random.nextInt(10) match {
        case 1 => formatter format n
        case _ => n.toString
      }
    })
  }

  def intsLargerThanMaxValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x > Int.MaxValue)

  def intsSmallerThanMinValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x < Int.MinValue)

  def nonNumerics: Gen[String] =
    alphaStr suchThat (_.nonEmpty)

  def decimals: Gen[String] =
    arbitrary[BigDecimal]
      .suchThat(_.abs < Int.MaxValue)
      .suchThat(!_.isValidInt)
      .map(_.formatted("%f"))

  def intsBelowValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ < value)

  def intsAboveValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ > value)

  def intsOutsideRange(min: Int, max: Int): Gen[Int] =
    arbitrary[Int] suchThat (x => x < min || x > max)

  def nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat(_.nonEmpty)
      .suchThat(_ != "true")
      .suchThat(_ != "false")

  def nonEmptyString: Gen[String] =
    arbitrary[String] suchThat (_.nonEmpty)

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars <- listOfN(length, regexWildcardChar)
    } yield chars.mkString

  def stringsStartingAlphaWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      first <- Gen.alphaChar
      chars <- listOfN(length - 1, regexWildcardChar)
    } yield (first :: chars).mkString.trim

  def numbersWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      num <- listOfN(length, numChar)
    } yield num.mkString

  def stringsLongerThan(minLength: Int): Gen[String] =
    for {
      base <- Gen.listOfN(minLength + 1, alphaNumChar).map(_.mkString)
      surplus <- alphaNumStr
    } yield base + surplus

  def stringsExceptSpecificValues(excluded: Seq[String]): Gen[String] =
    nonEmptyString suchThat (!excluded.contains(_))

  def historicDate(yearsBack: Int = 100): Gen[String] = {
    val to = LocalDate.now().minusDays(1)
    val from = LocalDate.of(to.getYear - yearsBack, 1, 1)

    Gen.choose(from.toEpochDay, to.toEpochDay)
      .map(LocalDate.ofEpochDay(_).format(formatter))
  }

  def regexWildcardChar: Gen[Char] = {
    arbitrary[Char].retryUntil(c =>
      c.getType match {
        case Character.LINE_SEPARATOR => false
        case Character.PARAGRAPH_SEPARATOR => false
        case Character.CONTROL => false
        case _ => true
      }
    )
  }

}
