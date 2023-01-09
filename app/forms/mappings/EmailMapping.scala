/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.mappings

import play.api.data.Mapping
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid

trait EmailMapping extends Mappings with Transforms {

  import EmailMapping._

  def emailMapping(requiredKey: String = requiredKey,
                   maxLengthKey: String = maxLengthKey,
                   invalidKey: String = invalidKey): Mapping[String] = {

    text(requiredKey).transform(
      standardTextTransform,
      noTransform).
      verifying(
        firstError(
          maxLength(EmailMapping.maxLengthEmail, maxLengthKey),
          emailFormat(noAtSignIncludedErrorKey, startsWithAtSignErrorKey, dotAfterAtSignErrorKey, endsWithDotErrorKey),
          email(invalidKey)
        )
      )
  }

  private def emailFormat(noAtSignIncludedErrorKey: String,
                          startsWithAtSignErrorKey: String,
                          dotAfterAtSignErrorKey: String,
                          endsWithDotErrorKey: String): Constraint[String] = {
    Constraint {
      case str if !str.contains("@") =>
        Invalid(noAtSignIncludedErrorKey)
      case str if str.startsWith("@") =>
        Invalid(startsWithAtSignErrorKey)
      case str if str.substring(str.indexOf("@") + 1).startsWith(".") =>
        Invalid(dotAfterAtSignErrorKey)
      case str if str.endsWith(".") =>
        Invalid(endsWithDotErrorKey)
      case _ =>
        Valid
    }
  }
}

object EmailMapping {
  val maxLengthEmail = 132

  val requiredKey = "messages__error__common__email__address__required"
  val invalidKey = "messages__error__common__email__address__invalid"
  val maxLengthKey = "messages__error__common__email__address__length"

  val noAtSignIncludedErrorKey = "messages__error__common__email__no_at_sign"
  val startsWithAtSignErrorKey = "messages__error__common__email__start_with_at_sign"
  val dotAfterAtSignErrorKey = "messages__error__common__email__dot_after_at_sign"
  val endsWithDotErrorKey = "messages__error__common__email__ends_with_dot"
}
