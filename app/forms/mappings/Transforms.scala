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

package forms.mappings

import scala.annotation.tailrec

trait Transforms {

  def noTransform(value: String): String = {
    value
  }

  def standardTextTransform(value: String): String = {
    value.trim
  }

  def ninoTransform(value: String): String = {
    strip(value).toUpperCase
  }

  def payeTransform(value: String): String = {
    value.replaceAll("[\\\\/]", "").trim
  }

  protected def strip(value: String): String = {
    value.replaceAll(" ", "")
  }

  @tailrec
  protected final def minimiseSpace(value: String): String = {
    if (value.contains("  ")) {
      minimiseSpace(value.replaceAll("  ", " "))
    } else {
      value
    }
  }

}
