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

import config.FrontendAppConfig
import connectors.{ListOfSchemesConnector, UserAnswersCacheConnector}
import controllers.Retrievals
import controllers.actions._
import forms.remove.RemovalDateFormProvider
import identifiers.SchemeSrnId
import identifiers.invitations.{PSANameId, SchemeNameId}
import identifiers.remove.RemovalDateId
import javax.inject.Inject
import models.{ListOfSchemes, NormalMode}
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.annotations.Invitation
import utils.{Navigator, UserAnswers}
import views.html.remove.removalDate

import scala.concurrent.Future

class RemovalDateController @Inject()(appConfig: FrontendAppConfig,
                                              override val messagesApi: MessagesApi,
                                              dataCacheConnector: UserAnswersCacheConnector,
                                              @Invitation navigator: Navigator,
                                              authenticate: AuthAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              formProvider: RemovalDateFormProvider,
                                              listSchemesConnector: ListOfSchemesConnector) extends FrontendController with I18nSupport with Retrievals {

  private def form(schemeOpenDate: LocalDate) = formProvider(schemeOpenDate)
  val earliestDate = LocalDate.parse("1900-01-01")

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSANameId and SchemeSrnId).retrieve.right.map {
        case schemeName ~ psaName ~ srn =>

          listSchemesConnector.getListOfSchemes(request.psaId.id).flatMap { list =>

             Future.successful(Ok(removalDate(appConfig, form(openedDate(srn, list)), psaName, schemeName, srn)))

          }
        case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      (SchemeNameId and PSANameId and SchemeSrnId).retrieve.right.map {
        case schemeName ~ psaName ~ srn =>
          listSchemesConnector.getListOfSchemes(request.psaId.id).flatMap { list =>

              form(openedDate(srn, list)).bindFromRequest().fold(
                (formWithErrors: Form[_]) =>
                  Future.successful(BadRequest(removalDate(appConfig, formWithErrors, psaName, schemeName, srn))),
                value =>
                  dataCacheConnector.save(request.externalId, RemovalDateId, value).map(cacheMap =>
                    Redirect(navigator.nextPage(RemovalDateId, NormalMode, UserAnswers(cacheMap))))
              )
          }
      }
  }


  private def openedDate(srn: String, list: ListOfSchemes): LocalDate = {
    list.schemeDetail.flatMap { listOfSchemes =>
      val currentScheme = listOfSchemes.filter(_.referenceNumber.contains(srn))
      if (currentScheme.nonEmpty) {
        currentScheme.head.openDate.map(new LocalDate(_))
      } else {
        None
      }
    }
  }.getOrElse(earliestDate)
}
