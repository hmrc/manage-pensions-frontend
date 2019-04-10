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

package controllers

import config.{FeatureSwitchManagementService, FrontendAppConfig}
import connectors.{ListOfSchemesConnector, PensionSchemeVarianceLockConnector, SchemeDetailsConnector, UserAnswersCacheConnector}
import controllers.actions._
import handlers.ErrorHandler
import identifiers.{ListOfPSADetailsId, SchemeNameId, SchemeSrnId}
import javax.inject.Inject
import models._
import models.requests.AuthenticatedRequest
import org.joda.time.LocalDate
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsArray, JsPath}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{DateHelper, Toggles, UserAnswers}
import viewmodels.AssociatedPsa
import views.html.schemeDetails

import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsController @Inject()(appConfig: FrontendAppConfig,
                                        override val messagesApi: MessagesApi,
                                        schemeDetailsConnector: SchemeDetailsConnector,
                                        listSchemesConnector: ListOfSchemesConnector,
                                        schemeVarianceLockConnector: PensionSchemeVarianceLockConnector,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        userAnswersCacheConnector: UserAnswersCacheConnector,
                                        errorHandler: ErrorHandler,
                                        featureSwitchManagementService: FeatureSwitchManagementService
                                       )(implicit val ec: ExecutionContext) extends FrontendController with I18nSupport {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = authenticate.async {
    implicit request =>
      if (featureSwitchManagementService.get(Toggles.isVariationsEnabled)) {
        onPageLoadVariations(srn)(request)
      } else {
        onPageLoadNonVariations(srn)(request)
      }
  }

  private def onPageLoadNonVariations(srn: SchemeReferenceNumber)(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    userAnswersCacheConnector.removeAll(request.externalId).flatMap { _ =>
      schemeDetailsConnector.getSchemeDetails(request.psaId.id, "srn", srn).flatMap { scheme =>
        if (scheme.psaDetails.toSeq.flatten.exists(_.id == request.psaId.id)) {
          listSchemesConnector.getListOfSchemes(request.psaId.id).flatMap { list =>
            val schemeDetail = scheme.schemeDetails
            val isSchemeOpen = schemeDetail.status.equalsIgnoreCase("open")
            userAnswersCacheConnector.save(request.externalId, SchemeSrnId, srn.id).flatMap { _ =>
              userAnswersCacheConnector.save(request.externalId, SchemeNameId, schemeDetail.name).map { _ =>
                Ok(schemeDetails(appConfig,
                  schemeDetail.name,
                  openedDate(srn.id, list, isSchemeOpen),
                  administrators(request.psaId.id, scheme),
                  srn.id,
                  isSchemeOpen,
                  false
                ))
              }
            }
          }
        } else {
          Future.successful(NotFound(errorHandler.notFoundTemplate))
        }
      }
    }

  private def onPageLoadVariations(srn: SchemeReferenceNumber)(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    withSchemeAndLock(srn).flatMap { result =>

        val isLocked = result._2 match {
          case Some(VarianceLock) | None => false
          case Some(_) => true
        }

        val admins = result._1.json.transform((JsPath \ 'psaDetails).json.pick)
          .asOpt.map(_.as[JsArray].value).toSeq.flatten
          .flatMap(_.transform((JsPath \ "id").json.pick).asOpt.flatMap(_.validate[String].asOpt).toSeq)

        val schemeStatus = result._1.json.transform((JsPath \ "schemeStatus").json.pick).asOpt.flatMap(_.validate[String].asOpt).getOrElse("")
        val schemeName = result._1.get(SchemeNameId).getOrElse("")

        if (admins.contains(request.psaId.id)) {
          listSchemesConnector.getListOfSchemes(request.psaId.id).flatMap { list =>
            val isSchemeOpen = schemeStatus.equalsIgnoreCase("open")
            userAnswersCacheConnector.save(request.externalId, SchemeSrnId, srn.id).flatMap { _ =>
              userAnswersCacheConnector.save(request.externalId, SchemeNameId, schemeName).map { _ =>
                Ok(schemeDetails(appConfig,
                  schemeName,
                  openedDate(srn.id, list, isSchemeOpen),
                  administratorsVariations(request.psaId.id, result._1, schemeStatus),
                  srn.id,
                  isSchemeOpen,
                  isLocked
                ))
              }
            }
          }
        } else {
          Future.successful(NotFound(errorHandler.notFoundTemplate))
        }
    }


  private def withSchemeAndLock(srn: SchemeReferenceNumber)(implicit request: AuthenticatedRequest[AnyContent]) = {
    for{
      _ <- userAnswersCacheConnector.removeAll(request.externalId)
      scheme <- schemeDetailsConnector.getSchemeDetailsVariations(request.psaId.id, "srn", srn)
      lock <- schemeVarianceLockConnector.isLockByPsaIdOrSchemeId(request.psaId.id, srn.id)
    } yield {
      (scheme, lock)
    }
  }
  private def administratorsVariations(psaId: String, psaSchemeDetails: UserAnswers, schemeStatus:String): Option[Seq[AssociatedPsa]] =
    psaSchemeDetails.get(ListOfPSADetailsId).map { psaDetailsSeq =>
      psaDetailsSeq.map { psaDetails =>
        val name = PsaDetails.getPsaName(psaDetails).getOrElse("")
        val canRemove = psaDetails.id.equals(psaId) && PsaSchemeDetails.canRemovePsaVariations(psaId, psaDetailsSeq, schemeStatus)
        AssociatedPsa(name, canRemove)
      }
    }

  private def administrators(psaId: String, psaSchemeDetails: PsaSchemeDetails): Option[Seq[AssociatedPsa]] =
    psaSchemeDetails.psaDetails.map(
      _.flatMap {
        psa =>
          PsaDetails.getPsaName(psa).map {
            name =>
              val canRemove = psa.id.equals(psaId) && PsaSchemeDetails.canRemovePsa(psaId, psaSchemeDetails)
              AssociatedPsa(name, canRemove)
          }
      }
    )

  private def openedDate(srn: String, list: ListOfSchemes, isSchemeOpen: Boolean): Option[String] = {
    if (isSchemeOpen) {
      list.schemeDetail.flatMap { listOfSchemes =>
        val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
        if (currentScheme.nonEmpty) {
          currentScheme.head.openDate.map(new LocalDate(_).toString(DateHelper.formatter))
        } else {
          None
        }
      }
    }
    else {
      None
    }
  }

}
