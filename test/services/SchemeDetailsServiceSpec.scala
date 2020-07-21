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

package services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import base.SpecBase
import connectors.FrontendConnector
import connectors.admin.MinimalPsaConnector
import connectors.scheme.PensionSchemeVarianceLockConnector
import identifiers.invitations.PSTRId
import identifiers.{SchemeNameId, SchemeStatusId}
import models.SchemeStatus.{Open, Rejected}
import models._
import models.requests.AuthenticatedRequest
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.AnyContent
import play.twirl.api.Html
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers
import viewmodels.AssociatedPsa

import scala.concurrent.Future

class SchemeDetailsServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  import SchemeDetailsServiceSpec._

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val authReq: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(fakeRequest, "id", PsaId(psaId), Individual)
  private val minimalPsaConnector: MinimalPsaConnector = mock[MinimalPsaConnector]
  private val lockConnector = mock[PensionSchemeVarianceLockConnector]
  private val frontendConnector = mock[FrontendConnector]

  def service: SchemeDetailsService =
    new SchemeDetailsService(frontendAppConfig, frontendConnector, lockConnector, minimalPsaConnector)

  "retrieveOptionAFTViewModel" must {
    "return model fron aft-frontend is Scheme status is open" in {

      when(frontendConnector.retrieveAftPartial(any())(any(), any()))
        .thenReturn(Future.successful(Html("test-aft-html")))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Open.value)).asOpt.get

      whenReady(service.retrieveAftHtml(ua, srn)) {
        _ mustBe Html("test-aft-html")
      }
    }

    "return None when the scheme status is other than Open/Wound-up/Deregistered" in {
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Rejected.value)).asOpt.get

      whenReady(service.retrieveAftHtml(ua, srn)) {
        _ mustBe Html("")
      }
    }
  }

  "retrievePaymentsAndChargesHtml" must {
    "return the Html for payments and charges" in {
      when(frontendConnector.retrievePaymentsAndChargesPartial(any())(any(), any()))
        .thenReturn(Future.successful(Html("test-payments-and-charges-html")))

      whenReady(service.retrievePaymentsAndChargesHtml(srn)) {
        _ mustBe Html("test-payments-and-charges-html")
      }
    }
  }

  "displayChangeLink" must {
    "return true if Lock is VarianceLock" in {
      service.displayChangeLink(isSchemeOpen = true, Some(VarianceLock)) mustBe true
    }

    "return true if lock is None" in {
      service.displayChangeLink(isSchemeOpen = true, None) mustBe true
    }

    "return false if lock is any lock other than VarianceLock" in {
      service.displayChangeLink(isSchemeOpen = true, Some(BothLock)) mustBe false
    }

    "return false if lock is scheme is not open" in {
      service.displayChangeLink(isSchemeOpen = false, Some(BothLock)) mustBe false
    }
  }

  "administratorVariations" must {
    "return a list of associated psas with canRemove status" in {
       val result  = service.administratorsVariations(psaId, userAnswersWithAssociatedPsa, "Open")
        result mustBe administrators
      }
  }

  "openedDate" must {
    "return an opened date if scheme is open and srn number matches a reference number in the list of schemes" in {
      val result = service.openedDate(srn, listOfSchemes, isSchemeOpen = true)
      result mustBe Some("1 January 2020")
    }

    "return None if scheme is not open" in {
      val result = service.openedDate(srn, listOfSchemes, isSchemeOpen = false)
      result mustBe None
    }

    "return None is srn does not match any reference number in the list" in {
      val result = service.openedDate("wrong-srn", listOfSchemes, isSchemeOpen = true)
      result mustBe None
    }
  }

  "pstr" must {
    "return a value if srn number matches a reference number in the list of schemes" in {
      val result = service.pstr(srn, listOfSchemes)
      result mustBe Some(pstr)
    }

    "return None is srn does not match any reference number in the list" in {
      val result = service.pstr("wrong-srn", listOfSchemes)
      result mustBe None
    }
  }

  "lockingPsa" must {
    "return the name of the locking psa" in {
      when(lockConnector.getLockByScheme(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(SchemeVariance("A0000001", srn))))
      when(minimalPsaConnector.getPsaNameFromPsaID(Matchers.eq("A0000001"))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some("Locky Lockhart")))
      whenReady(service.lockingPsa(Some(SchemeLock), srn)(authReq, hc)) {
        _ mustBe Some("Locky Lockhart")
      }
    }

    "return None when " in {
      when(lockConnector.getLockByScheme(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(SchemeVariance(psaId, "S1000000456"))))
      whenReady(service.lockingPsa(Some(SchemeLock), srn)(authReq, hc)){
        _ mustBe None
      }
    }
  }

}

object SchemeDetailsServiceSpec {

  private val srn = "srn"
  private val pstr = "pstr"
  private val psaId = "A0000000"
  private val name = "test-name"
  private val date = "2020-01-01"
  val minimalPsaName: Option[String] = Some("John Doe Doe")
  val administrators: Option[Seq[AssociatedPsa]] =
    Some(
      Seq(
        AssociatedPsa("partnership name 2", canRemove = true),
        AssociatedPsa("Tony A Smith", canRemove = false)
      )
    )
  val userAnswersWithAssociatedPsa: UserAnswers = UserAnswers(Json.obj(
    PSTRId.toString -> pstr,
    "schemeStatus" -> "Open",
    SchemeNameId.toString -> "scheme name",
    "psaDetails" -> JsArray(
      Seq(
        Json.obj(
          "id" -> "A0000000",
          "organisationOrPartnershipName" -> "partnership name 2",
          "relationshipDate" -> "2018-07-01"
        ),
        Json.obj(
          "id" -> "A0000001",
          "individual" -> Json.obj(
            "firstName" -> "Tony",
            "middleName" -> "A",
            "lastName" -> "Smith"
          ),
          "relationshipDate" -> "2018-07-01"
        )
      )
    )
  ))
  val listOfSchemes: ListOfSchemes = ListOfSchemes("", "", Some(List(SchemeDetail(name, srn, "Open", Some(date), Some(pstr), None))))

}
