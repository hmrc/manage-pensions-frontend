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
import config.FeatureSwitchManagementService
import connectors.{MinimalPsaConnector, SchemeDetailsConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.invitations.{PSANameId, PSTRId}
import identifiers.{SchemeNameId, SchemeSrnId}
import models.requests.DataRequest
import models.{MinimalPSA, SchemeDetails}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Toggles

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemovePsaController @Inject()(authenticate: AuthAction,
                                    val getData: DataRetrievalAction,
                                    val requireData: DataRequiredAction,
                                    schemeDetailsConnector: SchemeDetailsConnector,
                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                    minimalPsaConnector: MinimalPsaConnector,
                                    featureSwitchManagementService: FeatureSwitchManagementService
                                   )(
                                     implicit val ec: ExecutionContext) extends FrontendController with Retrievals {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      SchemeSrnId.retrieve.right.map { srn =>
        minimalPsaConnector.getMinimalPsaDetails(request.psaId.id).flatMap { minimalPsaDetails =>
          if (minimalPsaDetails.isPsaSuspended) {
            Future.successful(Redirect(controllers.remove.routes.CanNotBeRemovedController.onPageLoadWhereSuspended()))
          } else {
            if (featureSwitchManagementService.get(Toggles.isVariationsEnabled)) {
              renderPageVariations(request, srn, minimalPsaDetails)
            } else {
              renderPage(request, srn, minimalPsaDetails)
            }
          }
        }
      }
  }

  def renderPage(request: DataRequest[AnyContent], srn: String, minimalPsaDetails: MinimalPSA)(implicit hd: HeaderCarrier): Future[Result] = {
    import identifiers.invitations.{PSANameId, PSTRId, SchemeNameId}
    for {
      scheme <- schemeDetailsConnector.getSchemeDetails(request.psaId.id, "srn", srn)
      _ <- userAnswersCacheConnector.save(request.externalId, PSANameId, getPsaName(minimalPsaDetails))
      _ <- userAnswersCacheConnector.save(request.externalId, SchemeNameId, scheme.schemeDetails.name)
      _ <- userAnswersCacheConnector.save(request.externalId, PSTRId, getPstr(scheme.schemeDetails))
    } yield {
      Redirect(controllers.remove.routes.ConfirmRemovePsaController.onPageLoad())
    }
  }

  def renderPageVariations(request: DataRequest[AnyContent], srn: String, minimalPsaDetails: MinimalPSA)(implicit hd: HeaderCarrier): Future[Result] = {
    schemeDetailsConnector.getSchemeDetailsVariations(request.psaId.id, "srn", srn).flatMap { ua =>
      val schemeName = ua.get(SchemeNameId).getOrElse(throw new IllegalArgumentException("Organisation or Individual PSA Name missing in retrieved data"))
      val pstr = ua.get(PSTRId).getOrElse(throw new IllegalArgumentException("PSTR missing in retrieved data while removing PSA"))
      userAnswersCacheConnector.save(request.externalId, PSANameId, getPsaName(minimalPsaDetails)).map { _ =>
        import identifiers.invitations.SchemeNameId
        userAnswersCacheConnector.save(request.externalId, SchemeNameId, schemeName)
      }.flatMap { _ =>
        userAnswersCacheConnector.save(request.externalId, PSTRId, pstr).flatMap { _ =>
          userAnswersCacheConnector.save(request.externalId, SchemeSrnId, srn)
        }
      }
    }.map(_ =>
      Redirect(controllers.remove.routes.ConfirmRemovePsaController.onPageLoad())
    )
  }

  private def getPsaName(minimalPsaDetails: MinimalPSA): String = {
    (minimalPsaDetails.individualDetails, minimalPsaDetails.organisationName) match {
      case (Some(individualDetails), _) => individualDetails.fullName
      case (_, Some(orgName)) => orgName
      case _ => throw new IllegalArgumentException("Organisation or Individual PSA Name missing")
    }
  }

  private def getPstr(schemeDetails: SchemeDetails): String =
    schemeDetails.pstr.getOrElse(throw new IllegalArgumentException("PSTR missing while removing PSA"))
}
