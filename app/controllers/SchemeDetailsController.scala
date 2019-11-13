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

import java.time.LocalDate

import config.{FeatureSwitchManagementService, FrontendAppConfig}
import connectors._
import controllers.actions._
import handlers.ErrorHandler
import identifiers.{ListOfPSADetailsId, SchemeNameId, SchemeSrnId, SchemeStatusId}
import javax.inject.Inject
import models._
import models.requests.AuthenticatedRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendBaseController, FrontendController}
import utils.{DateHelper, UserAnswers}
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
                                        featureSwitchManagementService: FeatureSwitchManagementService,
                                        minimalPsaConnector: MinimalPsaConnector,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: schemeDetails
                                       )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: SchemeReferenceNumber): Action[AnyContent] = authenticate.async {
    implicit request =>
      withSchemeAndLock(srn).flatMap {
        case (userAnswers, lock) =>
          val schemeStatus = userAnswers.get(SchemeStatusId).getOrElse("")
          val isSchemeOpen = schemeStatus.equalsIgnoreCase("open")

          val displayChangeLink = {
            if (!isSchemeOpen) {
              false
            } else {
              lock match {
                case Some(VarianceLock) | None => true
                case Some(_) => false
              }
            }
          }

          val admins = (userAnswers.json \ "psaDetails").as[Seq[PsaDetails]].map(_.id)
          val schemeName = userAnswers.get(SchemeNameId).getOrElse("")

          if (admins.contains(request.psaId.id)) {
            listSchemesConnector.getListOfSchemes(request.psaId.id).flatMap { list =>
              userAnswersCacheConnector.save(request.externalId, SchemeSrnId, srn.id).flatMap { _ =>
                userAnswersCacheConnector.save(request.externalId, SchemeNameId, schemeName).flatMap { _ =>
                  lockingPsa(lock, srn).map { lockingPsa =>
                    Ok(view(
                      schemeName,
                      pstr(srn.id, list),
                      openedDate(srn.id, list, isSchemeOpen),
                      administratorsVariations(request.psaId.id, userAnswers, schemeStatus),
                      srn.id,
                      isSchemeOpen,
                      displayChangeLink,
                      lockingPsa
                    ))
                  }
                }
              }
            }
          } else {
            Future.successful(NotFound(errorHandler.notFoundTemplate))
          }
      }
  }


  private def withSchemeAndLock(srn: SchemeReferenceNumber)(implicit request: AuthenticatedRequest[AnyContent]) = {
    for {
      _ <- userAnswersCacheConnector.removeAll(request.externalId)
      scheme <- schemeDetailsConnector.getSchemeDetails(request.psaId.id, "srn", srn)
      lock <- schemeVarianceLockConnector.isLockByPsaIdOrSchemeId(request.psaId.id, srn.id)
    } yield {
      (scheme, lock)
    }
  }

  private def administratorsVariations(psaId: String, psaSchemeDetails: UserAnswers, schemeStatus: String): Option[Seq[AssociatedPsa]] =
    psaSchemeDetails.get(ListOfPSADetailsId).map { psaDetailsSeq =>
      psaDetailsSeq.map { psaDetails =>
        val name = PsaDetails.getPsaName(psaDetails).getOrElse("")
        val canRemove = psaDetails.id.equals(psaId) && PsaSchemeDetails.canRemovePsaVariations(psaId, psaDetailsSeq, schemeStatus)
        AssociatedPsa(name, canRemove)
      }
    }


  private def openedDate(srn: String, list: ListOfSchemes, isSchemeOpen: Boolean): Option[String] = {
    if (isSchemeOpen) {
      list.schemeDetail.flatMap {
        listOfSchemes =>
          val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
          if (currentScheme.nonEmpty) {
            currentScheme.head.openDate.map(date => LocalDate.parse(date).format(DateHelper.formatter))
          } else {
            None
          }
      }
    }
    else {
      None
    }
  }

  private def pstr(srn: String, list: ListOfSchemes): Option[String] =
    list.schemeDetail.flatMap { listOfSchemes =>
      val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
      if (currentScheme.nonEmpty) {
        currentScheme.head.pstr
      } else {
        None
      }
    }

  private def lockingPsa(lock: Option[Lock], srn: SchemeReferenceNumber)
                        (implicit request: AuthenticatedRequest[AnyContent]): Future[Option[String]] =
    lock match {
      case Some(SchemeLock) => schemeVarianceLockConnector.getLockByScheme(srn) flatMap {
        case Some(schemeVariance) if !(schemeVariance.psaId == request.psaId.id) =>
          minimalPsaConnector.getPsaNameFromPsaID(schemeVariance.psaId).map(identity)
        case _ => Future.successful(None)
      }
      case _ => Future.successful(None)
    }

}
