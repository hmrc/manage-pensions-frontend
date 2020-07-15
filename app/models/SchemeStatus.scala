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

package models

/*
  Possible values of Scheme Status:
    Pending
    Pending Info Required
    Pending Info Received
    Rejected
    Open
    Deregistered
    Wound-up
    Rejected Under Appeal

 */

sealed trait SchemeStatus {
  def value: String
  def pending: Boolean
  def rejected: Boolean
  def canRemovePsa: Boolean
}

abstract class AbstractSchemeStatus(
  override val value: String,
  override val canRemovePsa: Boolean,
  override val pending: Boolean,
  override val rejected: Boolean
) extends SchemeStatus

object SchemeStatus {

  case object Pending extends AbstractSchemeStatus("Pending", false, true, false)

  case object PendingInfoRequired extends AbstractSchemeStatus("Pending Info Required", false, true, false)

  case object PendingInfoReceived extends AbstractSchemeStatus("Pending Info Received", false, true, false)

  case object Rejected extends AbstractSchemeStatus("Rejected", true, false, true)

  case object Open extends AbstractSchemeStatus("Open", true, false, false)

  case object Deregistered extends AbstractSchemeStatus("Deregistered", false, false, false)

  case object WoundUp extends AbstractSchemeStatus("Wound-up", true, false, false)

  case object RejectedUnderAppeal extends AbstractSchemeStatus("Rejected Under Appeal", false, false, true)

  lazy val statuses: Seq[SchemeStatus] =
    Seq(
      Pending,
      PendingInfoRequired,
      PendingInfoReceived,
      Rejected,
      Open,
      Deregistered,
      WoundUp,
      RejectedUnderAppeal
    )

  lazy val values: Seq[String] =
    statuses.map(_.value)

  def forValue(value: String): SchemeStatus =
    statuses
      .find(status => status.value.equals(value))
      .getOrElse(throw new IllegalArgumentException(s"Unknown scheme status: $value"))

}
