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
import connectors.admin.MinimalPsaConnector
import connectors.aft.{AFTConnector, AftCacheConnector}
import connectors.scheme.PensionSchemeVarianceLockConnector
import identifiers.{SchemeNameId, SchemeStatusId}
import identifiers.invitations.PSTRId
import models.SchemeStatus.{Open, Rejected}
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
import utils.DateHelper.{endDateFormat, startDateFormat}
import utils.{DateHelper, UserAnswers}
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

  private val version1 = AFTVersion(1, LocalDate.now())
  private val version2 = AFTVersion(2, LocalDate.now())
  private val versions = Seq(version1, version2)

  def service: SchemeDetailsService =
    new SchemeDetailsService(frontendAppConfig, aftConnector, aftCacheConnector,
      lockConnector, minimalPsaConnector)

  "retrieveOptionAFTViewModel after overviewApiEnablement" must {
    "return overview api returns multiple returns in progress, multiple past returns and start link needs to be displayed" in {
      DateHelper.setDate(Some(LocalDate.of(2021,4,1)))
      when(aftConnector.getAftOverview(any())(any(), any()))
        .thenReturn(Future.successful(allTypesMultipleReturnsPresent))
      when(aftConnector.aftStartDate).thenReturn(LocalDate.of(2020, 4, 1))
      when(aftConnector.aftEndDate).thenReturn(LocalDate.of(2021, 6, 30))

      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Open.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe allTypesMultipleReturnsModel
      }
    }

    "return the correct model when return no returns are in progress" in {
      when(aftConnector.getAftOverview(any())(any(), any()))
        .thenReturn(Future.successful(noInProgress))
      when(aftConnector.aftStartDate).thenReturn(LocalDate.of(2020, 4, 1))
      when(aftConnector.aftEndDate).thenReturn(LocalDate.of(2021, 6, 30))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Open.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe noInProgressModel
      }
    }

    "return the correct model when return one return is in progress but not locked" in {
      when(aftConnector.getAftOverview(any())(any(), any()))
        .thenReturn(Future.successful(oneInProgress))
      when(aftConnector.aftStartDate).thenReturn(LocalDate.of(2020, 4, 1))
      when(aftConnector.aftEndDate).thenReturn(LocalDate.of(2021, 6, 30))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Open.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe oneInProgressModelNotLocked
      }
    }

    "return the correct model when one return is in progress and locked by another user" in {
      when(aftConnector.getAftOverview(any())(any(), any()))
        .thenReturn(Future.successful(oneInProgress))
      when(aftConnector.aftStartDate).thenReturn(LocalDate.of(2020, 4, 1))
      when(aftConnector.aftEndDate).thenReturn(LocalDate.of(2021, 6, 30))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(name)))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Open.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe oneInProgressModelLocked
      }
    }

    "return None when the scheme status is other than Open/Wound-up/Deregistered" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(version1))))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Rejected.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe Nil
      }
    }
  }

  "retrieveOptionAFTViewModel before overviewApiEnablement" must {
    "return the correct model when return is locked by another credentials" in {
      DateHelper.setDate(Some(LocalDate.of(2020,4,1)))
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(version1))))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(name)))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Open.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe lockedAftModel
      }
    }

    "return the correct model when return is not locked but versions is empty" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Nil)))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Open.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe unlockedEmptyAftModel
      }
    }

    "return the correct model when return is locked but versions is empty" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Nil)))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(name)))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Open.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe lockedAftModelWithNoVersion
      }
    }

    "return the correct model when return is in progress but not locked" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(version1))))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Open.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe inProgressUnlockedAftModel
      }
    }

    "return None when the scheme status is other than Open/Wound-up/Deregistered" in {
      when(aftConnector.getListOfVersions(any())(any(), any()))
        .thenReturn(Future.successful(Some(Seq(version1))))
      when(aftCacheConnector.lockedBy(any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      val ua = UserAnswers().set(PSTRId)(pstr).flatMap(_.set(SchemeStatusId)(Rejected.value)).asOpt.get

      whenReady(service.retrieveOptionAFTViewModel(ua, srn)) {
        _ mustBe Nil
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
  private val startDate = "2020-04-01"
  private val endDate = "2020-06-30"
  private val dateFormatterYMD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  private val formattedStartDate: String = LocalDate.parse(startDate, dateFormatterYMD).format(DateTimeFormatter.ofPattern("d MMMM"))
  private val formattedEndDate: String = LocalDate.parse(endDate, dateFormatterYMD).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
  private val srn = "srn"
  private val pstr = "pstr"
  private val psaId = "A0000000"
  private val name = "test-name"
  private val date = "2020-01-01"
  val minimalPsaName: Option[String] = Some("John Doe Doe")
  val lockedAftModel: Seq[AFTViewModel] = Seq(
    AFTViewModel(
      Some(Message("messages__schemeDetails__aft_period", formattedStartDate, formattedEndDate)),
      Some(Message("messages__schemeDetails__aft_lockedBy", name)),
      Link(
        id = "aftSummaryPageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/$startDate/1/summary",
        linkText = Message("messages__schemeDetails__aft_view"))
    )
  )
  val unlockedEmptyAftModel: Seq[AFTViewModel] = Seq(
    AFTViewModel(
      None,
      None,
      Link(
        id = "aftChargeTypePageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/aft-login",
        linkText = Message("messages__schemeDetails__aft_startLink", formattedStartDate, formattedEndDate))
    )
  )
  val lockedAftModelWithNoVersion: Seq[AFTViewModel] = Seq(
    AFTViewModel(
      Some(Message("messages__schemeDetails__aft_period", formattedStartDate, formattedEndDate)),
      Some(Message("messages__schemeDetails__aft_lockedBy", name)),
      Link(
        id = "aftSummaryPageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/$startDate/summary",
        linkText = Message("messages__schemeDetails__aft_view"))
    )
  )
  val inProgressUnlockedAftModel: Seq[AFTViewModel] = Seq(
    AFTViewModel(
      Some(Message("messages__schemeDetails__aft_period", formattedStartDate, formattedEndDate)),
      Some(Message("messages__schemeDetails__aft_inProgress")),
      Link(
        id = "aftSummaryPageLink",
        url = s"http://localhost:8206/manage-pension-scheme-accounting-for-tax/$srn/new-return/$startDate/1/summary",
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

  val overviewApril20: AFTOverview = AFTOverview(
    LocalDate.of(2020, 4, 1),
    LocalDate.of(2020, 6, 30),
    2,
    submittedVersionAvailable = true,
    compiledVersionAvailable = false
  )

  val overviewJuly20: AFTOverview = AFTOverview(
    LocalDate.of(2020, 7, 1),
    LocalDate.of(2020, 9, 30),
    2,
    submittedVersionAvailable = true,
    compiledVersionAvailable = false
  )

  val overviewOctober20: AFTOverview = AFTOverview(
    LocalDate.of(2020, 10, 1),
    LocalDate.of(2020, 12, 31),
    2,
    submittedVersionAvailable = true,
    compiledVersionAvailable = true
  )

  val overviewJan21: AFTOverview = AFTOverview(
    LocalDate.of(2021, 1, 1),
    LocalDate.of(2021, 3, 31),
    2,
    submittedVersionAvailable = true,
    compiledVersionAvailable = true
  )

  val aftLoginUrl: String = "http://localhost:8206/manage-pension-scheme-accounting-for-tax/srn/new-return/aft-login"
  val amendUrl: String = "http://localhost:8206/manage-pension-scheme-accounting-for-tax/srn/previous-return/amend-select"
  val returnHistoryUrl: String = "http://localhost:8206/manage-pension-scheme-accounting-for-tax/srn/previous-return/2020-10-01/amend-previous"

  val startModel: AFTViewModel = AFTViewModel(None, None,
    Link(id = "aftLoginLink", url = aftLoginUrl,
      linkText = Message("messages__schemeDetails__aft_start")))

  val pastReturnsModel: AFTViewModel = AFTViewModel(None, None,
    Link(
      id = "aftAmendLink",
      url = amendUrl,
      linkText = Message("messages__schemeDetails__aft_view_or_change")))

  def multipleInProgressModel(count: Int): AFTViewModel = AFTViewModel(
    Some(Message("messages__schemeDetails__aft_multiple_inProgress")),
    Some(Message("messages__schemeDetails__aft_inProgressCount").withArgs(count)),
    Link(
      id = "aftAmendLink",
      url = amendUrl,
      linkText = Message("messages__schemeDetails__aft_view"))
  )

  def oneInProgressModel(locked: Boolean): AFTViewModel = AFTViewModel(
    Some(Message("messages__schemeDetails__aft_period", "1 October", "31 December 2020")),
    if (locked) {
      Some(Message("messages__schemeDetails__aft_lockedBy", name))
    }
    else {
      Some(Message("messages__schemeDetails__aft_inProgress"))
    },
    Link(id = "aftReturnHistoryLink", url = returnHistoryUrl,
      linkText = Message("messages__schemeDetails__aft_view"))
  )

  val allTypesMultipleReturnsPresent = Seq(overviewApril20, overviewJuly20, overviewOctober20, overviewJan21)
  val noInProgress = Seq(overviewApril20, overviewJuly20)
  val oneInProgress = Seq(overviewApril20 , overviewOctober20)

  val allTypesMultipleReturnsModel = Seq(multipleInProgressModel(2), startModel, pastReturnsModel)
  val noInProgressModel = Seq(startModel, pastReturnsModel)
  val oneInProgressModelLocked = Seq(oneInProgressModel(locked = true), startModel, pastReturnsModel)
  val oneInProgressModelNotLocked = Seq(oneInProgressModel(locked = false), startModel, pastReturnsModel)

}
