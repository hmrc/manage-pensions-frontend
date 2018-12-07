/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.{MinimalPsaConnector, SchemeDetailsConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.SchemeSrnId
import identifiers.invitations.{PSANameId, PSTRId, SchemeNameId}
import models.{MinimalPSA, SchemeDetails}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class RemovePsaController @Inject()(authenticate: AuthAction,
                                    val getData: DataRetrievalAction,
                                    val requireData: DataRequiredAction,
                                    schemeDetailsConnector: SchemeDetailsConnector,
                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                    minimalPsaConnector: MinimalPsaConnector) extends FrontendController with Retrievals {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      SchemeSrnId.retrieve.right.map { srn =>
        minimalPsaConnector.getMinimalPsaDetails(request.psaId.id).flatMap { minimalPsaDetails =>
          if (minimalPsaDetails.isPsaSuspended) {
            Future.successful(Redirect(controllers.remove.routes.CanNotBeRemovedController.onPageLoadWhereSuspended()))
          } else {
            for {
              scheme <- schemeDetailsConnector.getSchemeDetails(request.psaId.id, "srn", srn)
              _ <- userAnswersCacheConnector.save(request.externalId, PSANameId, getPsaName(minimalPsaDetails))
              _ <- userAnswersCacheConnector.save(request.externalId, SchemeNameId, scheme.schemeDetails.name)
              _ <- userAnswersCacheConnector.save(request.externalId, PSTRId, getPstr(scheme.schemeDetails))
            } yield {
              Redirect(controllers.remove.routes.ConfirmRemovePsaController.onPageLoad())
            }
          }
        }
      }
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
