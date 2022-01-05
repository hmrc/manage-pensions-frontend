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

package viewmodels

import play.api.i18n.Messages

import scala.language.implicitConversions

sealed trait Message {
  def resolve(implicit messages: Messages): String

  def withArgs(args: Any*): Message
}

object Message {

  def apply(key: String, args: Any*): Message =
    Resolvable(key, args)

  case class Resolvable(key: String, args: Seq[Any]) extends Message {

    override def resolve(implicit messages: Messages): String =
      messages(key, args: _*)

    override def withArgs(args: Any*): Message =
      copy(args = args)
  }

  case class Literal(value: String) extends Message {

    override def resolve(implicit messages: Messages): String =
      value

    // should this log a warning?
    override def withArgs(args: Any*): Message = this
  }

  implicit def literal(string: String): Message = Literal(string)

  implicit def resolve(message: Message)(implicit messages: Messages): String =
    message.resolve

  implicit def resolveOption(message: Option[Message])(implicit messages: Messages): Option[String] =
    message.map(_.resolve)
}
