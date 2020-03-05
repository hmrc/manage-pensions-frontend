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

package controllers

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import connectors.admin.MinimalPsaConnector
import connectors.scheme.ListOfSchemesConnector
import controllers.actions.{AuthAction, DataRetrievalAction}
import identifiers.PSANameId
import models.requests.OptionalDataRequest
import models.{Index, ListOfSchemes, SchemeDetail}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PaginationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.list_schemes

import scala.concurrent.{ExecutionContext, Future}

class ListSchemesController @Inject()(
                                       val appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       listSchemesConnector: ListOfSchemesConnector,
                                       minimalPsaConnector: MinimalPsaConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: list_schemes,
                                       paginationService: PaginationService
                                     )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val pagination: Int = appConfig.listSchemePagination

  def listOfSchemes(implicit hc: HeaderCarrier,request: OptionalDataRequest[AnyContent]): Future[ListOfSchemes] =
    listSchemesConnector.getListOfSchemes(request.psaId.id)

  def schemeDetails(listOfSchemes: ListOfSchemes): List[SchemeDetail] =
    listOfSchemes.schemeDetail.getOrElse(List.empty[SchemeDetail])

  def renderView(schemeDetails: List[SchemeDetail], numberOfSchemes: Int, pageNumber: Int, numberOfPages: Int)
                (implicit hc: HeaderCarrier, request: OptionalDataRequest[AnyContent]): Future[Result] = {

    minimalPsaConnector.getPsaNameFromPsaID(request.psaId.id).flatMap(_.map {
      name =>
        userAnswersCacheConnector.save(request.externalId, PSANameId, name).map {
          _ =>
            Ok(view(
              schemes = schemeDetails,
              psaName = name,
              numberOfSchemes = numberOfSchemes,
              pagination = pagination,
              pageNumber = pageNumber,
              pageNumberLinks = paginationService.pageNumberLinks(pageNumber, numberOfSchemes, pagination, numberOfPages),
              numberOfPages = numberOfPages
            ))
        }
    }.getOrElse {
      Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
    })
  }

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      listOfSchemes.flatMap {
        listOfSchemes =>
          val numberOfSchemes: Int = schemeDetails(listOfSchemes).length

          val numberOfPages: Int = paginationService.divide(numberOfSchemes, pagination)

          renderView(
            schemeDetails = schemeDetails(listOfSchemes).take(pagination),
            numberOfSchemes = numberOfSchemes,
            pageNumber = 1,
            numberOfPages = numberOfPages
          )
      }
  }

  def onPageLoadWithPageNumber(pageNumber: Index): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      listOfSchemes.flatMap {
        listOfSchemes =>
          val numberOfSchemes: Int = schemeDetails(listOfSchemes).length

          val numberOfPages: Int = paginationService.divide(numberOfSchemes, pagination)

          if (pageNumber <= numberOfPages)
            renderView(
              schemeDetails = schemeDetails(listOfSchemes).slice((pageNumber * pagination) - pagination, pageNumber * pagination),
              numberOfSchemes = numberOfSchemes,
              pageNumber = pageNumber,
              numberOfPages = numberOfPages
            )
          else
            Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }
}
