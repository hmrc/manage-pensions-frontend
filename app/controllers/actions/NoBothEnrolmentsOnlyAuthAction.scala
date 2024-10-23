/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.actions

import config.FrontendAppConfig
import controllers.routes._
import models.requests.IdentifiedRequest
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NoBothEnrolmentsOnlyAuthAction @Inject()(
                                   override val authConnector: AuthConnector,
                                   config: FrontendAppConfig,
                                   val parser: BodyParsers.Default
                                 )(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[NoBothEnrolmentsRequest, AnyContent]
    with AuthorisedFunctions {

  // scalastyle:off
  override def invokeBlock[A](request: Request[A], block: NoBothEnrolmentsRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(
      Retrievals.externalId and Retrievals.allEnrolments
    ) {
      case Some(id) ~ enrolments =>
        if(podsBothEnrolmentsExist(enrolments)) {
          Future.successful(Redirect(controllers.routes.SchemesOverviewController.onPageLoad()))
        } else {
          block(NoBothEnrolmentsRequest(request, id, AuthAction.getPsaId(isMandatory = false, enrolments), AuthAction.getPspId(isMandatory = false, enrolments)))
        }

      case _ =>
        Future.successful(Redirect(UnauthorisedController.onPageLoad))
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: InsufficientEnrolments =>
        Redirect(UnauthorisedController.onPageLoad)
      case _: InsufficientConfidenceLevel =>
        Redirect(UnauthorisedController.onPageLoad)
      case _: UnsupportedAuthProvider =>
        Redirect(UnauthorisedController.onPageLoad)
      case _: UnsupportedAffinityGroup =>
        Redirect(UnauthorisedController.onPageLoad)
      case _: UnsupportedCredentialRole =>
        Redirect(UnauthorisedController.onPageLoad)
    }
  }

  private def podsBothEnrolmentsExist(enrolments: Enrolments): Boolean = {
    AuthAction.getPsaId(isMandatory = false, enrolments).isDefined && AuthAction.getPspId(isMandatory = false, enrolments).isDefined
  }

}

case class NoBothEnrolmentsRequest[A](request: Request[A],
                                   externalId: String,
                                   psaId: Option[PsaId],
                                   pspId: Option[PspId] = None)
  extends WrappedRequest[A](request) with IdentifiedRequest




