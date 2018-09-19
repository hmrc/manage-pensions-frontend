package controllers.invitation

import com.google.inject.Inject
import config.FrontendAppConfig
import forms.invitation.PsaIdFromProvider
import models.NormalMode
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Results, AnyContent, Action}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import views.html.invitation.psaId
import scala.concurrent.Future


class PsaIdController@Inject()(config:FrontendAppConfig,
                               override val messagesApi: MessagesApi) extends BaseController with I18nSupport {

  def get() : Action[AnyContent] = Action.async{ implicit request =>


    val formProvider = new PsaIdFromProvider()
    val form = formProvider()

    Future.successful(Ok(psaId(config,form, NormalMode)))

  }
}