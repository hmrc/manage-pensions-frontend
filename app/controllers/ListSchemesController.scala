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
import forms.ListSchemesFormProvider
import identifiers.PSANameId
import models.requests.OptionalDataRequest
import models.{ListOfSchemes, SchemeDetail, Index}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Result, AnyContent, MessagesControllerComponents, Action}
import services.PaginationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.list_schemes

import scala.concurrent.{Future, ExecutionContext}

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
                                       formProvider:ListSchemesFormProvider
                                     )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val pagination: Int = appConfig.listSchemePagination

  private val form: Form[String] = formProvider()

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
              form,
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

  private def test(func:List[SchemeDetail]=>List[SchemeDetail] = identity)(implicit request:OptionalDataRequest[AnyContent]):Future[Result] = {
    listOfSchemes.flatMap {
      listOfSchemes =>
        val numberOfSchemes: Int = func(schemeDetails(listOfSchemes)).length

        val numberOfPages: Int = paginationService.divide(numberOfSchemes, pagination)

        renderView(
          schemeDetails = schemeDetails(listOfSchemes).take(pagination),
          numberOfSchemes = numberOfSchemes,
          pageNumber = 1,
          numberOfPages = numberOfPages
        )
    }
  }

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      test()
  }

  private val srnRegex = "^S[0-9]{10}$".r
  private val pstrRegex = "^[0-9]{8}[A-Za-z]{2}$".r

  val func: (String, List[SchemeDetail]) => List[SchemeDetail] = (searchText, list) => {
    searchText match {
      case srn if srnRegex.findFirstIn(searchText).isDefined =>
      println( "\n>>SRN:" + srn)
        list.filter(_.referenceNumber.startsWith(searchText))
      case pstr if pstrRegex.findFirstIn(searchText).isDefined =>

        println( "\n>>>" + list)

        println( "\n>>PSTR:" + pstr + "...")

        list.filter(_.pstr.exists(_ == searchText))
      case _ =>
      println( "\nNOPE")
        list
    }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest("WAAA")), value =>
        {
          test(func(value, _:List[SchemeDetail]))
        }
      )
  }

  def onPageLoadWithPageNumber(pageNumber: Index): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      listOfSchemes.flatMap {
        listOfSchemes =>
          val numberOfSchemes: Int = schemeDetails(listOfSchemes).length

          val numberOfPages: Int = paginationService.divide(numberOfSchemes, pagination)

          if (pageNumber > 0 && pageNumber <= numberOfPages)
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
