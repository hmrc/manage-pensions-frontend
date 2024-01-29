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
import connectors.admin.{DelimitedAdminException, MinimalConnector}
import connectors.scheme.SchemeDetailsConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.psa.remove.routes.{CanNotBeRemovedController, ConfirmRemovePsaController}
import controllers.routes._
import identifiers.invitations.PSTRId
import identifiers.psa.PSANameId
import identifiers.{AssociatedDateId, SchemeNameId, SchemeSrnId}
import models.MinimalPSAPSP
import models.psa.PsaAssociatedDate
import models.requests.DataRequest
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsArray, JsPath, __}
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
                                     val controllerComponents: MessagesControllerComponents
                                   )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals {

  def onPageLoad: Action[AnyContent] = (authenticate() andThen getData andThen requireData).async {
    implicit request =>
      SchemeSrnId.retrieve.map { srn =>
        minimalPsaConnector.getMinimalPsaDetails(request.psaIdOrException.id).flatMap { minimalPsaDetails =>
          if (minimalPsaDetails.isPsaSuspended) {
            Future.successful(Redirect(CanNotBeRemovedController.onPageLoadWhereSuspended()))
          } else if (minimalPsaDetails.deceasedFlag) {
            Future.successful(Redirect(ContactHMRCController.onPageLoad()))
          } else if (minimalPsaDetails.rlsFlag) {
            Future.successful(Redirect(appConfig.psaUpdateContactDetailsUrl))
          } else {
            renderPage(request, srn, minimalPsaDetails)
          }
        } recoverWith {
          case _: DelimitedAdminException =>
            Future.successful(Redirect(controllers.routes.DelimitedAdministratorController.onPageLoad))
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
      Redirect(ConfirmRemovePsaController.onPageLoad())
    }
  }

  private def getPsaName(minimalPsaDetails: MinimalPSAPSP): String = {
    (minimalPsaDetails.individualDetails, minimalPsaDetails.organisationName) match {
      case (Some(individualDetails), _) => individualDetails.fullName
      case (_, Some(orgName)) => orgName
      case _ => throw new IllegalArgumentException("Organisation or Individual PSA Name missing")
    }
  }

  private def getPstr(pstr: Option[String]): String =
    pstr.getOrElse(throw new IllegalArgumentException("PSTR missing while removing PSA"))

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
        userAnswers.get(SchemeNameId).getOrElse(throw new IllegalArgumentException("SchemeName missing while removing PSA")),
        getPstr(userAnswers.get(PSTRId)),
        associatedDate.getOrElse(appConfig.earliestDatePsaRemoval)
      )
    }
  }


  case class SchemeInfo(schemeName: String, pstr: String, associatedDate: LocalDate)

}
