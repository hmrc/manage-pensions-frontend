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

package controllers.remove

import com.google.inject.{Inject, Singleton}
import config.{FeatureSwitchManagementService, FrontendAppConfig}
import connectors.{MinimalPsaConnector, SchemeDetailsConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.invitations.PSTRId
import identifiers.{AssociatedDateId, SchemeNameId, SchemeSrnId}
import models.{MinimalPSA, PsaAssociatedDate}
import models.requests.DataRequest
import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsArray, JsPath, __}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.{FrontendBaseController, FrontendController}
import utils.Toggles
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemovePsaController @Inject()(authenticate: AuthAction,
                                    val getData: DataRetrievalAction,
                                    val requireData: DataRequiredAction,
                                    schemeDetailsConnector: SchemeDetailsConnector,
                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                    minimalPsaConnector: MinimalPsaConnector,
                                    appConfig: FrontendAppConfig,
                                    val controllerComponents: MessagesControllerComponents
                                   )(
                                     implicit val ec: ExecutionContext) extends FrontendBaseController with Retrievals {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      SchemeSrnId.retrieve.right.map { srn =>
        minimalPsaConnector.getMinimalPsaDetails(request.psaId.id).flatMap { minimalPsaDetails =>
          if (minimalPsaDetails.isPsaSuspended) {
            Future.successful(Redirect(controllers.remove.routes.CanNotBeRemovedController.onPageLoadWhereSuspended()))
          } else {
            renderPage(request, srn, minimalPsaDetails)
          }
        }
      }
  }

  private def renderPage(request: DataRequest[AnyContent], srn: String, minimalPsaDetails: MinimalPSA)(implicit hd: HeaderCarrier): Future[Result] = {
    import identifiers.invitations.{PSANameId, PSTRId, SchemeNameId}
    for {
      scheme <- getSchemeNameAndPstr(srn, request)
      _ <- userAnswersCacheConnector.save(request.externalId, PSANameId, getPsaName(minimalPsaDetails))
      _ <- userAnswersCacheConnector.save(request.externalId, SchemeNameId, scheme.schemeName)
      _ <- userAnswersCacheConnector.save(request.externalId, PSTRId, scheme.pstr)
      _ <- userAnswersCacheConnector.save(request.externalId, AssociatedDateId, scheme.associatedDate)
    } yield {
      Redirect(controllers.remove.routes.ConfirmRemovePsaController.onPageLoad())
    }
  }

  private def getPsaName(minimalPsaDetails: MinimalPSA): String = {
    (minimalPsaDetails.individualDetails, minimalPsaDetails.organisationName) match {
      case (Some(individualDetails), _) => individualDetails.fullName
      case (_, Some(orgName)) => orgName
      case _ => throw new IllegalArgumentException("Organisation or Individual PSA Name missing")
    }
  }

  private def getPstr(pstr: Option[String]): String =
    pstr.getOrElse(throw new IllegalArgumentException("PSTR missing while removing PSA"))

  private def getSchemeNameAndPstr(srn: String, request: DataRequest[AnyContent])(implicit hd: HeaderCarrier): Future[SchemeInfo] = {
      schemeDetailsConnector.getSchemeDetails(request.psaId.id, "srn", srn).map{ userAnswers =>

        val admins = userAnswers.json.transform((JsPath \ 'psaDetails).json.pick)
          .asOpt.map(_.as[JsArray].value).toSeq.flatten
          .flatMap(_.transform((
            (__ \ 'psaId).json.copyFrom((JsPath \ "id").json.pick) and
              (__ \ 'relationshipDate).json.copyFrom((JsPath \ 'relationshipDate).json.pick)
            ).reduce).asOpt.flatMap(_.validate[PsaAssociatedDate].asOpt))

        val psa = admins.filter(_.psaId.contains(request.psaId.id))

        val associatedDate = if (psa.nonEmpty) {
          psa.head.relationshipDate.map(new LocalDate(_))
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


  case class SchemeInfo(schemeName:String, pstr:String, associatedDate:LocalDate)
}
