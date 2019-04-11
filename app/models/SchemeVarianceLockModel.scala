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

package models

import play.api.libs.json._
import utils.Enumerable

import scala.language.implicitConversions


class WithName(string: String) {
  override val toString: String = string
}

case class SchemeVariance(psaId: String, srn: String)

object SchemeVariance {
  implicit val format: OFormat[SchemeVariance] = Json.format[SchemeVariance]
}

sealed trait Lock

case object VarianceLock extends WithName("SuccessfulVarianceLock") with Lock
case object PsaLock extends WithName("PsaHasLockedAnotherScheme") with Lock
case object SchemeLock extends WithName("AnotherPsaHasLockedScheme") with Lock
case object BothLock extends WithName("PsaAndSchemeHasAlreadyLocked") with Lock


object Lock extends Enumerable.Implicits {

  override def toString: String = super.toString.toLowerCase

  implicit object LockFormat extends Format[Lock] {

    implicit def reads(json: JsValue) : JsResult[Lock] =
      json match {
        case JsString("SuccessfulVarianceLock") => JsSuccess(VarianceLock)
        case JsString("PsaHasLockedAnotherScheme") => JsSuccess(PsaLock)
        case JsString("AnotherPsaHasLockedScheme") => JsSuccess(SchemeLock)
        case JsString("PsaAndSchemeHasAlreadyLocked") => JsSuccess(BothLock)
        case _ => JsError("cannot parse it")
      }

    implicit def writes(lock: Lock) = JsString(lock.toString)

  }

  implicit val enumerable: Enumerable[Lock] = Enumerable(
    Seq(VarianceLock, PsaLock, SchemeLock).map(v => v.toString -> v): _*
  )
}
