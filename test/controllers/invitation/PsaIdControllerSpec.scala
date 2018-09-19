package controllers.invitation

import controllers.ControllerSpecBase
import forms.invitation.PsaIdFromProvider
import models.NormalMode
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.helper.form
import views.html.invitation.psaId

class PsaIdControllerSpec extends ControllerSpecBase {

  val formProvider = new PsaIdFromProvider()
  val form = formProvider()

  def viewAsString(form: Form[_] = form) = psaId(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString

  "Calling get" must {

    "return 200 " in {

      val controller =  new PsaIdController(frontendAppConfig, messagesApi)
      val result =  controller.get()(FakeRequest())

      status(result) mustBe 200
      contentAsString(result) mustBe viewAsString()
    }

  }

}