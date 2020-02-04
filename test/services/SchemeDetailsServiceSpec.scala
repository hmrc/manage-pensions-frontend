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

import base.SpecBase
import connectors.admin.MinimalPsaConnector
import connectors.aft.{AFTConnector, AftCacheConnector}
import connectors.scheme.PensionSchemeVarianceLockConnector
import identifiers.SchemeNameId
import identifiers.invitations.PSTRId
import models.requests.AuthenticatedRequest
import models._
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers
import viewmodels.{AFTViewModel, AssociatedPsa, Message}

import scala.concurrent.Future

class SchemeDetailsServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  import SchemeDetailsServiceSpec._

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val authReq: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(fakeRequest, "id", PsaId(psaId), Individual, "test-id")
  private val minimalPsaConnector: MinimalPsaConnector = mock[MinimalPsaConnector]
  private val lockConnector = mock[PensionSchemeVarianceLockConnector]
  private val aftConnector = mock[AFTConnector]

  private val aftCacheConnector = mock[AftCacheConnector]

  def service: SchemeDetailsService =
    new SchemeDetailsService(frontendAppConfig, aftConnector, aftCacheConnector,
      lockConnector, minimalPsaConnector)

  "retrieveOptionAFTViewModel" must {
    "return the correct model when return is locked by another credentials" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(1))))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(name)))
      val ua = UserAnswers().set(PSTRId)(pstr).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe lockedAftModel
      }
    }

    "return the correct model when return is not locked but versions is empty" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Nil)))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val ua = UserAnswers().set(PSTRId)(pstr).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe unlockedEmptyAftModel
      }
    }

    "return the correct model when return is locked but versions is empty" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Nil)))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(name)))
      val ua = UserAnswers().set(PSTRId)(pstr).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe lockedAftModelWithNoVersion
      }
    }

    "return the correct model when return is in progress but not locked" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(1))))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val ua = UserAnswers().set(PSTRId)(pstr).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe inProgressUnlockedAftModel
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


  val lockedAftModel: Option[AFTViewModel] = Some(
    AFTViewModel(
      Some(Message("messages__schemeDetails__aft_period")),
      Some(Message("messages__schemeDetails__aft_lockedBy", name)),
      Link(
        id = "aftSummaryPageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/1/summary",
        linkText = Message("messages__schemeDetails__aft_view"))
    )
  )

  val unlockedEmptyAftModel: Option[AFTViewModel] = Some(
    AFTViewModel(
      None,
      None,
      Link(
        id = "aftChargeTypePageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/charge-type",
        linkText = Message("messages__schemeDetails__aft_startLink"))
    )
  )

  val lockedAftModelWithNoVersion: Option[AFTViewModel] = Some(
    AFTViewModel(
      Some(Message("messages__schemeDetails__aft_period")),
      Some(Message("messages__schemeDetails__aft_lockedBy", name)),
      Link(
        id = "aftSummaryPageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/summary",
        linkText = Message("messages__schemeDetails__aft_view"))
    )
  )

  val inProgressUnlockedAftModel: Option[AFTViewModel] = Option(
    AFTViewModel(
      Some(Message("messages__schemeDetails__aft_period")),
      Some(Message("messages__schemeDetails__aft_inProgress")),
      Link(
        id = "aftSummaryPageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/1/summary",
        linkText = Message("messages__schemeDetails__aft_view"))
    )
  )

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
