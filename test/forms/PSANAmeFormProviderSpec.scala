package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class PSANAmeFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "pSANAme.error.required"
  val lengthKey = "pSANAme.error.length"
  val maxLength = 107

  val form = new PsaNameFormProvider()()

  ".value" must {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
