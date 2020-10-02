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

package controllers

import base.SpecBase
import controllers.actions.FakeDataRetrievalAction
import identifiers.PSANameId
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json

trait ControllerSpecBase extends SpecBase {

  val cacheMapId = "id"

  def getEmptyData: FakeDataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj()))

  def dontGetAnyData: FakeDataRetrievalAction = new FakeDataRetrievalAction(None)

  def getDataWithPsaName(psaId: String = "A0000000"): FakeDataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj(
    PSANameId.toString -> "Test Psa Name"
  )), psaId)

  protected def applicationBuilder(modules: Seq[GuiceableModule] = Seq.empty): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(modules: _*)

}
