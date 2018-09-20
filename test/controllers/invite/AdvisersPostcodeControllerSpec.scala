package controllers.invite

import controllers._
import controllers.actions._
import models.NormalMode
import play.api.data.Form
import play.api.mvc.Call
import views.html.adviserPostcode

class AdvisersPostcodeControllerSpec extends ControllerSpecBase {
  def onwardRoute = Call("GET", "/foo")

  val formProvider = new AdviserPostcodeFormProvider()
  val form = formProvider()

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) = new AdvisersPostcodeController()

  def viewAsString(form: Form[_] = form) = adviserPostcode(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString

}
