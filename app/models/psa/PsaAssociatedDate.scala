/*
 * Copyright 2024 HM Revenue & Customs
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

package models.psa

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Reads, Writes, Json}


case class PsaAssociatedDate(psaId : String, relationshipDate:Option[String])

object PsaAssociatedDate{

  implicit val reads: Reads[PsaAssociatedDate] =
    (
      (JsPath \ "psaId").read[String].orElse((JsPath \ "id").read[String]) and
      (JsPath \ "relationshipDate").readNullable[String]
      )((psaId, relationshipDate) => PsaAssociatedDate(psaId, relationshipDate))

  implicit val writes: Writes[PsaAssociatedDate] = Json.writes[PsaAssociatedDate]

}
