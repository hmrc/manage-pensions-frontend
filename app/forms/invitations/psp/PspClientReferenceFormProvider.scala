/*
 * Copyright 2022 HM Revenue & Customs
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
import models.ClientReference
import models.ClientReference.{HaveClientReference, NoClientReference}
import play.api.data.Forms.tuple
import play.api.data.{Form, Mapping}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class PspClientReferenceFormProvider @Inject() extends Mappings with Transforms {

  def apply(): Form[ClientReference] = Form(
    clientReferenceMapping
  )

  protected def clientReferenceMapping: Mapping[ClientReference] = {
    val clientRefMaxLength = 11

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
      "hasReference" -> boolean("messages__clientReference_yes_no_required"),
      "reference" -> mandatoryIfTrue("hasReference", text("messages__clientReference_required")
        .transform(strip, noTransform)
        .verifying(firstError(
          maxLength(clientRefMaxLength, "messages__clientReference_maxLength"),
          clientRef("messages__clientReference_invalid")
        )
        ))
    ).transform(toClientReference, fromClientReference)
  }
}
