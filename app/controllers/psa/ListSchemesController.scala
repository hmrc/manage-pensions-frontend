/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.psa

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import connectors.admin.{DelimitedAdminException, MinimalConnector}
import controllers.actions.{AuthAction, DataRetrievalAction}
import forms.psa.ListSchemesFormProvider
import identifiers.psa.PSANameId
import models.requests.OptionalDataRequest
import models.{Index, MinimalPSAPSP, SchemeDetails}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{PaginationService, SchemeSearchService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SortSchemes
import views.html.psa.list_schemes

import scala.concurrent.{ExecutionContext, Future}

class ListSchemesController @Inject()(
                                       val appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       minimalPsaConnector: MinimalConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: list_schemes,
                                       paginationService: PaginationService,
                                       formProvider: ListSchemesFormProvider,
                                       schemeSearchService: SchemeSearchService,
                                       sortSchemes: SortSchemes
                                     )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  private val pagination: Int = appConfig.listSchemePagination

  private val form: Form[String] = formProvider()

  private def renderView(
                          schemeDetails: List[SchemeDetails],
                          numberOfSchemes: Int,
                          pageNumber: Int,
                          numberOfPages: Int,
                          noResultsMessageKey: Option[String],
                          form: Form[_]
                        )(implicit hc: HeaderCarrier,
                          request: OptionalDataRequest[AnyContent]): Future[Result] = {
    val status = if (form.hasErrors) BadRequest else Ok
    val sortedSchemes = sortSchemes.sort(schemeDetails)

    minimalPsaConnector.getMinimalPsaDetails(request.psaIdOrException.id).flatMap { minimalDetails =>
      (minimalDetails, MinimalPSAPSP.getNameFromId(minimalDetails)) match {
        case (md, _) if md.deceasedFlag => Future.successful(Redirect(controllers.routes.ContactHMRCController.onPageLoad()))
        case (md, _) if md.rlsFlag => Future.successful(Redirect(appConfig.psaUpdateContactDetailsUrl))
        case (_, Some(name)) =>
          userAnswersCacheConnector.save(request.externalId, PSANameId, name).map { _ =>
            status(
              view(
                form,
                schemes = sortedSchemes,
                psaName = name,
                numberOfSchemes = numberOfSchemes,
                pagination = pagination,
                pageNumber = pageNumber,
                pageNumberLinks = paginationService.pageNumberLinks(
                  pageNumber,
                  numberOfSchemes,
                  pagination,
                  numberOfPages
                ),
                numberOfPages = numberOfPages,
                noResultsMessageKey
              )
            )
          }
        case (_, None) => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad))
      }
    } recoverWith {
      case _: DelimitedAdminException =>
        Future.successful(Redirect(controllers.routes.DelimitedAdministratorController.onPageLoad))
    }
  }

  private def searchAndRenderView(
                                   form: Form[_],
                                   pageNumber: Int,
                                   searchText: Option[String]
                                 )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    schemeSearchService.search(request.psaIdOrException.id, searchText).flatMap { searchResult =>

      val noResultsMessageKey =
        (searchText.isDefined, searchResult.isEmpty) match {
          case (true, true) =>
            Some("messages__listSchemes__search_noMatches")
          case (false, true) => Some("messages__listSchemes__noSchemes")
          case _ => None
        }

      val numberOfSchemes: Int = searchResult.length

      val numberOfPages: Int =
        paginationService.divide(numberOfSchemes, pagination)

      selectPageOfResults(searchResult, pageNumber, numberOfPages) match {
        case Some(searchResultToRender) =>
          renderView(
            schemeDetails = searchResultToRender,
            numberOfSchemes = numberOfSchemes,
            pageNumber = pageNumber,
            numberOfPages = numberOfPages,
            noResultsMessageKey = noResultsMessageKey,
            form = form
          )
        case _ =>
          Future.successful(
            Redirect(controllers.routes.SessionExpiredController.onPageLoad)
          )
      }
    }
  }

  private def selectPageOfResults(
                                   searchResult: List[SchemeDetails],
                                   pageNumber: Int,
                                   numberOfPages: Int
                                 ): Option[List[SchemeDetails]] = {
    pageNumber match {
      case 1 => Some(searchResult.take(pagination))
      case p if p <= numberOfPages =>
        Some(
          searchResult.slice(
            (pageNumber * pagination) - pagination,
            pageNumber * pagination
          )
        )
      case _ =>
        None
    }
  }

  def onPageLoad: Action[AnyContent] = (authenticate() andThen getData).async {
    implicit request =>
      searchAndRenderView(searchText = None, pageNumber = 1, form = form)
  }

  def onPageLoadWithPageNumber(pageNumber: Index): Action[AnyContent] =
    (authenticate() andThen getData).async { implicit request =>
      searchAndRenderView(
        searchText = None,
        pageNumber = pageNumber,
        form = form
      )
    }

  def onSearch: Action[AnyContent] = (authenticate() andThen getData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[_]) =>
            searchAndRenderView(
              searchText = None,
              pageNumber = 1,
              form = formWithErrors
            ),
          value => {
            searchAndRenderView(
              searchText = Some(value),
              pageNumber = 1,
              form = form.fill(value)
            )
          }
        )
  }
}
