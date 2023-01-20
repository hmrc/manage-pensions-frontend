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
import models.URBanner
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.{Form, FormError, Mapping}
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class UrBannerFormProviderSpec extends FormBehaviours with GuiceOneAppPerSuite with EmailBehaviours {

  private val regexPersonOrOrganisationName =   """^[a-zA-Z\u00C0-\u00FF '??\u2014\u2013\u2010\u002d]{1,107}"""
  private val email = "email"
  private val name = "indOrgName"
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val messages: Messages = messagesApi.preferred(FakeRequest())
  val validData: Map[String, String] = Map(
    name -> URBanner.apply("name", "email").indOrgName,
    email -> URBanner.apply("name", "email").email
  )
  private val requiredEmailKey = "messages__error__adviser__email__address__required"
  private val maxLengthEmailKey = "messages__error__adviser__email__address__length"
  private val invalidEmailKey = "messages__error__adviser__email__address__invalid"
  private val mapping: Mapping[String] = emailMapping(requiredEmailKey, maxLengthEmailKey, invalidEmailKey)
  private val formEmail = Form(
    email -> mapping
  )

  private val requiredNameKey = "messages__banner__error_required"
  private val invalidNameKey = "messages__banner__error"

  val form = new UrBannerFormProvider()()

  "UrBannerFormProvider" must {

    behave like formWithEmailField(formEmail, email, requiredEmailKey, maxLengthEmailKey, invalidEmailKey)

    behave like fieldThatBindsValidData(form, name, "Name")

    behave like mandatoryField(form, name, FormError(name, requiredNameKey))

    behave like fieldWithRegex(form, name, "!£$%^&*", FormError(name, invalidNameKey, Seq(regexPersonOrOrganisationName.toString)))
  }
}
