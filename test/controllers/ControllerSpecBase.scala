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

package controllers

import base.SpecBase
import connectors.scheme.SchemeDetailsConnector
import actions.{FakeDataRetrievalAction, FakePsaSchemeAuthAction, FakePspSchemeAuthAction}
import controllers.invitations.InviteControllerSpec.readJsonFromFile
import handlers.ErrorHandler
import identifiers.psa.PSANameId
import identifiers.psp.PSPNameId
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import uk.gov.hmrc.domain.PspId
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

trait ControllerSpecBase extends SpecBase {

  val cacheMapId = "id"
  val psp: Option[PspId] = Some(PspId("00000000"))
  val fakePsaSchemeAuthAction = new FakePsaSchemeAuthAction(app.injector.instanceOf[SchemeDetailsConnector], app.injector.instanceOf[ErrorHandler])
  val fakePspSchemeAuthAction: FakePspSchemeAuthAction =
    new FakePspSchemeAuthAction(app.injector.instanceOf[SchemeDetailsConnector], app.injector.instanceOf[ErrorHandler])



  def getEmptyData: FakeDataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj()))
  def getEmptyDataPsp: FakeDataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj()), pspId = psp)

  def dontGetAnyData: FakeDataRetrievalAction = new FakeDataRetrievalAction(None)
  def dontGetAnyDataPsp: FakeDataRetrievalAction = new FakeDataRetrievalAction(None, pspId = psp)

  def getDataWithPsaName(psaId: String = "A0000000"): FakeDataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj(
    PSANameId.toString -> "Test Psa Name"
  )), psaId)

  def getDataWithPspName(pspId: String = "00000000"): FakeDataRetrievalAction = new FakeDataRetrievalAction(Some(Json.obj(
    PSPNameId.toString -> "Test Pspa Name"
  )), pspId = Some(PspId(pspId)))

  protected def applicationBuilder(modules: Seq[GuiceableModule] = Seq.empty): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(modules: _*)

  def fakeSchemeDetailsConnector: SchemeDetailsConnector = new SchemeDetailsConnector {

    override def getSchemeDetails(psaId: String,
                                  idNumber: String,
                                  schemeIdType: String
                                 )(implicit hc: HeaderCarrier,
                                   ec: ExecutionContext): Future[UserAnswers] =
      Future.successful(UserAnswers(readJsonFromFile("/data/validSchemeDetailsUserAnswers.json")))

    override def getPspSchemeDetails(pspId: String, srn: String)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] = ???

    override def getSchemeDetailsRefresh(psaId: String,
                                         idNumber: String,
                                         schemeIdType: String)
                                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = ???

    override def isPsaAssociated(psaOrPspId: String,
                                 idType: String,
                                 srn: String)
                                (implicit hc: HeaderCarrier,
                                 ec: ExecutionContext): Future[Option[Boolean]] = ???
  }

}
