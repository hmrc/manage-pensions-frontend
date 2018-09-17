package views

import play.api.data.Form
import controllers.routes
import forms.PsaNameFormProvider
import models.NormalMode
import views.behaviours.StringViewBehaviours
import views.html.psaName

class PSANAmeViewSpec extends StringViewBehaviours {

  val messageKeyPrefix = "psa__name"

  override val form = new PsaNameFormProvider()().apply()

  def createView = () => psaName(frontendAppConfig, form, NormalMode)(fakeRequest, messages)

  def createViewUsingForm = (form: Form[String]) => psaName(frontendAppConfig, form, NormalMode)(fakeRequest, messages)

  "PsaName view" must {
    behave like normalPage(createView, messageKeyPrefix, messages(s"messages__${messageKeyPrefix}__title"))

    behave like pageWithBackLink(createView)

    behave like stringPage(createViewUsingForm, messageKeyPrefix, routes.PsaNameController.onSubmit(NormalMode).url)
  }
}
