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

package forms

import forms.behaviours.{EmailBehaviours, FormBehaviours}
import models.{AdministratorOrPractitioner, Field, Invalid, Required, URBanner}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.data.Form
import play.api.data.Mapping

class UrBannerFormProviderSpec extends FormBehaviours with GuiceOneAppPerSuite with EmailBehaviours {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages: Messages = messagesApi.preferred(FakeRequest())

  val validData: Map[String, String] = Map(
    "indOrgName" -> URBanner.apply("name", "email").indOrgName,
    "email" -> URBanner.apply("name", "email").email
  )
  private val requiredKey = "messages__error__adviser__email__address__required"
  private val maxLengthKey = "messages__error__adviser__email__address__length"
  private val invalidKey = "messages__error__adviser__email__address__invalid"

  private val mapping: Mapping[String] = emailMapping(requiredKey, maxLengthKey, invalidKey)
  private val fieldName = "email"

  private val formEmail = Form(
    fieldName -> mapping
  )

  val form = new UrBannerFormProvider()()

  "UrBannerFormProvider" must {

    behave like formWithEmailField(formEmail, fieldName, requiredKey, maxLengthKey, invalidKey)

    behave like questionForm[URBanner](URBanner.apply("name", "email"))

  }
}
