/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.psp

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.UserAnswersCacheConnector
import connectors.admin.MinimalConnector
import controllers.actions.AuthAction
import controllers.actions.DataRetrievalAction
import forms.psp.ListSchemesFormProvider
import identifiers.psa.PSANameId
import models.AuthEntity.PSP
import models.SchemeDetails
import models.requests.OptionalDataRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import services.SchemeSearchService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.psp.list_schemes

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ListSchemesController @Inject()(
                                       val appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       minimalConnector: MinimalConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: list_schemes,
                                       formProvider: ListSchemesFormProvider,
                                       schemeSearchService: SchemeSearchService
                                     )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  private val form: Form[String] = formProvider()

  private def renderView(
                          schemeDetails: List[SchemeDetails],
                          numberOfSchemes: Int,
                          form: Form[_]
                        )(implicit hc: HeaderCarrier,
                          request: OptionalDataRequest[AnyContent]): Future[Result] = {
    val status = if (form.hasErrors) BadRequest else Ok
    minimalConnector
      .getNameFromPspID(request.pspIdOrException.id)
      .flatMap(_.map {
        name =>
          userAnswersCacheConnector
            .save(request.externalId, PSANameId, name)
            .map { _ =>
              status(
                view(
                  form,
                  schemes = schemeDetails,
                  pspName = name,
                  numberOfSchemes = numberOfSchemes
                )
              )
            }
      }.getOrElse {
        Future.successful(
          Redirect(controllers.routes.SessionExpiredController.onPageLoad())
        )
      })
  }

  private def searchAndRenderView(
                                   form: Form[_],
                                   pageNumber: Int,
                                   searchText: Option[String]
                                 )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    schemeSearchService.searchPsp(request.pspIdOrException.id, searchText).flatMap { searchResult =>
          renderView(
            schemeDetails = searchResult,
            numberOfSchemes = searchResult.length,
            form = form
          )
    }
  }

  def onPageLoad: Action[AnyContent] = (authenticate(PSP) andThen getData).async {
    implicit request =>
      renderView(
        schemeDetails = Nil,
        numberOfSchemes = 0,
        form = form
      )

  }

  def onSearch: Action[AnyContent] = (authenticate(PSP) andThen getData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[_]) =>
            renderView(
              schemeDetails = Nil,
              numberOfSchemes = 0,
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
