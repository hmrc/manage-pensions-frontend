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

package controllers.psa.remove

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import connectors.admin.MinimalConnector
import connectors.scheme.SchemeDetailsConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction, PsaSchemeAuthAction}
import controllers.psa.remove.routes._
import controllers.routes._
import identifiers.invitations.PSTRId
import identifiers.psa.PSANameId
import identifiers.{AssociatedDateId, SchemeNameId, SchemeSrnId}
import models.{MinimalPSAPSP, SchemeReferenceNumber}
import models.psa.PsaAssociatedDate
import models.requests.DataRequest
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsArray, JsPath, __}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemovePsaController @Inject()(
                                     authenticate: AuthAction,
                                     val getData: DataRetrievalAction,
                                     val requireData: DataRequiredAction,
                                     schemeDetailsConnector: SchemeDetailsConnector,
                                     userAnswersCacheConnector: UserAnswersCacheConnector,
                                     minimalPsaConnector: MinimalConnector,
                                     appConfig: FrontendAppConfig,
                                     val controllerComponents: MessagesControllerComponents,
                                     psaSchemeAction: PsaSchemeAuthAction
                                   )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals {

  import RemovePsaController._

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] =
                (authenticate() andThen getData andThen psaSchemeAction(srn) andThen requireData).async {
    implicit request =>
      SchemeSrnId.retrieve.map { srnFetched =>
        minimalPsaConnector.getMinimalPsaDetails(request.psaIdOrException.id).flatMap { minimalPsaDetails =>
          if (minimalPsaDetails.isPsaSuspended) {
            Future.successful(Redirect(CanNotBeRemovedController.onPageLoadWhereSuspended()))
          } else if (minimalPsaDetails.deceasedFlag) {
            Future.successful(Redirect(ContactHMRCController.onPageLoad()))
          } else if (minimalPsaDetails.rlsFlag) {
            Future.successful(Redirect(appConfig.psaUpdateContactDetailsUrl))
          } else {
            renderPage(request, srnFetched, minimalPsaDetails)
          }
        }
      }
  }

  private def renderPage(request: DataRequest[AnyContent], srn: String, minimalPsaDetails: MinimalPSAPSP)
                        (implicit hd: HeaderCarrier): Future[Result] = {
    import identifiers.invitations.{PSTRId, SchemeNameId}
    for {
      scheme <- getSchemeNameAndPstr(srn, request)
      _ <- userAnswersCacheConnector.save(request.externalId, PSANameId, getPsaName(minimalPsaDetails))
      _ <- userAnswersCacheConnector.save(request.externalId, SchemeNameId, scheme.schemeName)
      _ <- userAnswersCacheConnector.save(request.externalId, PSTRId, scheme.pstr)
      _ <- userAnswersCacheConnector.save(request.externalId, AssociatedDateId, scheme.associatedDate)
    } yield {
      Redirect(ConfirmRemovePsaController.onPageLoad(srn))
    }
  } recoverWith renderPageRecoverAndRedirect

  private def getPsaName(minimalPsaDetails: MinimalPSAPSP): String = {
    (minimalPsaDetails.individualDetails, minimalPsaDetails.organisationName) match {
      case (Some(individualDetails), _) => individualDetails.fullName
      case (_, Some(orgName)) => orgName
      case _ => throw MissingPsaNameException("Organisation or Individual PSA Name missing")
    }
  }

  private def getPstr(pstr: Option[String]): String = pstr.getOrElse(throw MissingPstrException("PSTR missing while removing PSA"))

  private def getSchemeNameAndPstr(srn: String, request: DataRequest[AnyContent])
                                  (implicit hd: HeaderCarrier): Future[SchemeInfo] = {
    schemeDetailsConnector.getSchemeDetails(
      psaId = request.psaIdOrException.id,
      idNumber = srn,
      schemeIdType = "srn"
    ) map { userAnswers =>

      val admins = userAnswers.json.transform((JsPath \ Symbol("psaDetails")).json.pick)
        .asOpt.map(_.as[JsArray].value).toSeq.flatten
        .flatMap(_.transform((
          (__ \ Symbol("psaId")).json.copyFrom((JsPath \ "id").json.pick) and
            (__ \ Symbol("relationshipDate")).json.copyFrom((JsPath \ Symbol("relationshipDate")).json.pick)
          ).reduce).asOpt.flatMap(_.validate[PsaAssociatedDate].asOpt))

      val psa = admins.filter(_.psaId.contains(request.psaIdOrException.id))

      val associatedDate = if (psa.nonEmpty) {
        psa.head.relationshipDate.map(LocalDate.parse(_))
      } else {
        None
      }
      SchemeInfo(
        userAnswers.get(SchemeNameId).getOrElse(throw MissingSchemeNameException("SchemeName missing while removing PSA")),
        getPstr(userAnswers.get(PSTRId)),
        associatedDate.getOrElse(appConfig.earliestDatePsaRemoval)
      )
    }
  }


}

object RemovePsaController {

  private case class SchemeInfo(schemeName: String, pstr: String, associatedDate: LocalDate)

  private case class MissingPstrException(message: String) extends IllegalArgumentException
  private case class MissingPsaNameException(message: String) extends IllegalArgumentException
  private case class MissingSchemeNameException(message: String) extends IllegalArgumentException


  private val renderPageRecoverAndRedirect: PartialFunction[Throwable, Future[Result]] = {
    case _: MissingPstrException       => Future.successful(Redirect(MissingInfoController.onPageLoadPstr()))
    case _: MissingPsaNameException    => Future.successful(Redirect(MissingInfoController.onPageLoadOther()))
    case _: MissingSchemeNameException => Future.successful(Redirect(MissingInfoController.onPageLoadOther()))
  }
}
