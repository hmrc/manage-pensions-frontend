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
import controllers.actions.AuthAction
import controllers.actions.DataRetrievalAction
import forms.ListSchemesFormProvider
import identifiers.PSANameId
import models.requests.OptionalDataRequest
import models.Index
import models.ListOfSchemes
import models.SchemeDetail
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import services.PaginationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.list_schemes

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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
  paginationService: PaginationService,
  formProvider: ListSchemesFormProvider
)(implicit val ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val pagination: Int = appConfig.listSchemePagination

  private val form: Form[String] = formProvider()

  private def listOfSchemes(
    implicit hc: HeaderCarrier,
    request: OptionalDataRequest[AnyContent]
  ): Future[ListOfSchemes] =
    listSchemesConnector.getListOfSchemes(request.psaId.id)

  private def schemeDetails(listOfSchemes: ListOfSchemes): List[SchemeDetail] =
    listOfSchemes.schemeDetail.getOrElse(List.empty[SchemeDetail])

  private def renderView(
    schemeDetails: List[SchemeDetail],
    numberOfSchemes: Int,
    pageNumber: Int,
    numberOfPages: Int,
    searchText: Option[String]
  )(implicit hc: HeaderCarrier,
    request: OptionalDataRequest[AnyContent]): Future[Result] = {
    val filledForm = searchText.fold(form)(form.fill)

    minimalPsaConnector
      .getPsaNameFromPsaID(request.psaId.id)
      .flatMap(_.map {
        name =>
          userAnswersCacheConnector
            .save(request.externalId, PSANameId, name)
            .map { _ =>
              Ok(
                view(
                  filledForm,
                  schemes = schemeDetails,
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
                  numberOfPages = numberOfPages
                )
              )
            }
      }.getOrElse {
        Future.successful(
          Redirect(controllers.routes.SessionExpiredController.onPageLoad())
        )
      })
  }

  private val srnRegex = "^S[0-9]{10}$".r
  private val pstrRegex = "^[0-9]{8}[A-Za-z]{2}$".r

  private val filter: (String, List[SchemeDetail]) => List[SchemeDetail] =
    (searchText, list) => {
      searchText match {
        case srn if srnRegex.findFirstIn(searchText).isDefined =>
          list.filter(_.referenceNumber == searchText)
        case pstr if pstrRegex.findFirstIn(searchText).isDefined =>
          list.filter(_.pstr.exists(_ == searchText))
        case _ =>
          List.empty
      }
    }

  private def searchAndRenderView(
                                   searchText: Option[String],
                                   filter: List[SchemeDetail] => List[SchemeDetail],
                                   pageNumber: Int
                                 )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    listOfSchemes.flatMap { listOfSchemes =>
      val searchResult = filter(schemeDetails(listOfSchemes))
      val numberOfSchemes: Int = searchResult.length

      val numberOfPages: Int =
        paginationService.divide(numberOfSchemes, pagination)

      val optionSearchResultToRender = pageNumber match {
        case 1 => Some(searchResult.take(pagination))
        case p if p <= numberOfPages =>
          Some(searchResult.slice(
            (pageNumber * pagination) - pagination,
            pageNumber * pagination
          ))
        case _ =>
          None
      }

      optionSearchResultToRender match {
        case Some(searchResultToRender) =>
          renderView(
            searchText = searchText,
            schemeDetails = searchResultToRender,
            numberOfSchemes = numberOfSchemes,
            pageNumber = pageNumber,
            numberOfPages = numberOfPages
          )
        case _ =>
          Future.successful(
            Redirect(controllers.routes.SessionExpiredController.onPageLoad())
          )
      }
    }
  }

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      searchAndRenderView(
        searchText = None,
        filter = identity,
        pageNumber = 1
      )
  }

  def onSearch: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      val value = form
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[_]) => None,
          value => Some(value)
        )

      val f: List[SchemeDetail] => List[SchemeDetail] = value match {
        case Some(v) => filter(v, _: List[SchemeDetail])
        case _ => _ => List.empty
      }

      searchAndRenderView(
        searchText = value,
        filter = f,
        pageNumber = 1
      )
  }

  def onPageLoadWithPageNumber(pageNumber: Index): Action[AnyContent] =
    (authenticate andThen getData).async { implicit request =>
      searchAndRenderView(
        searchText = None,
        filter = identity,
        pageNumber = pageNumber
      )
    }
}
