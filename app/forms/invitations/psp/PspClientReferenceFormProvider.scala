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

package forms.invitations.psp

import forms.mappings.{Mappings, Transforms}
import javax.inject.Inject
import models.invitations.psp.ClientReference
import models.invitations.psp.ClientReference.{HaveClientReference, NoClientReference}
import play.api.data.{Form, Mapping}
import play.api.data.Forms.tuple
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

class PspClientReferenceFormProvider @Inject() extends Mappings with Transforms {


  def apply(): Form[ClientReference] = Form(
    "value" -> clientReferenceMapping
  )



  protected def clientReferenceMapping: Mapping[ClientReference] = {
    val clientRefMaxLength = 160

    def fromClientReference(clientReference: ClientReference): (Boolean, Option[String]) = {
      clientReference match {
        case ClientReference.HaveClientReference(clientRef) => (true, Some(clientRef))
        case _ => (false, None)
      }
    }

    def toClientReference(clientReferenceTuple: (Boolean, Option[String])): ClientReference =
      clientReferenceTuple match {
        case (_, Some(value)) => HaveClientReference(value)
        case _ =>  NoClientReference
      }

    tuple(
      "yesNo" -> boolean("messages__clientReference_yes_no_required"),
      "reference" -> mandatoryIfTrue("value.yesNo", text("messages__clientReference_required").
        verifying(firstError(
          maxLength(clientRefMaxLength, "messages__clientReference_maxLength"))))
    ).transform(toClientReference, fromClientReference)
  }
}



