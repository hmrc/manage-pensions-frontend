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

package config

import com.google.inject.{Inject, Singleton}
import controllers.routes
import org.joda.time.LocalDate
import play.api.Mode.Mode
import play.api.i18n.Lang
import play.api.mvc.Call
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {

  override protected def mode: Mode = environment.mode

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadConfigOrDefault(key: String, default: String) = runModeConfiguration.getString(key).getOrElse(default)

  private lazy val contactHost = runModeConfiguration.getString("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "managepensionsfrontend"

  lazy val appName: String = runModeConfiguration.underlying.getString("appName")
  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
  lazy val googleTagManagerIdAvailable: Boolean = runModeConfiguration.underlying.getBoolean(s"google-tag-manager.id-available")
  lazy val googleTagManagerId: String = loadConfig(s"google-tag-manager.id")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"

  lazy val authUrl: String = baseUrl("auth")
  lazy val pensionsSchemeUrl: String = baseUrl("pensions-scheme")
  lazy val pensionAdminUrl: String = baseUrl("pension-administrator")
  lazy val schemeFrontendUrl: String = baseUrl("pensions-scheme-frontend")

  lazy val loginUrl = loadConfig("urls.login")
  lazy val loginContinueUrl = loadConfig("urls.loginContinue")
  lazy val serviceSignOut = loadConfig("urls.logout")
  lazy val registerSchemeAdministratorUrl: String = loadConfig("urls.registerSchemeAdministrator")
  lazy val pensionAdministratorGovUkLink = runModeConfiguration.underlying.getString("urls.pensionAdministratorGovUkLink")
  lazy val pensionPractitionerGovUkLink = runModeConfiguration.underlying.getString("urls.pensionPractitionerGovUkLink")
  lazy val govUkLink = runModeConfiguration.underlying.getString("urls.govUkLink")
  lazy val continueSchemeUrl = s"${loadConfig("urls.continueSchemeRegistration")}"
  lazy val userResearchUrl = runModeConfiguration.underlying.getString("urls.userResearch")
  lazy val pensionSchemeOnlineServiceUrl: String = loadConfig("urls.pensionSchemeOnlineService")
  lazy val registeredPsaDetailsUrl: String = loadConfig("urls.psaDetails")

  lazy val languageTranslationEnabled: Boolean = runModeConfiguration.getBoolean("features.welsh-translation").getOrElse(true)
  lazy val isWorkPackageOneEnabled: Boolean = runModeConfiguration.getBoolean("features.work-package-one-enabled").getOrElse(false)
  lazy val registerSchemeUrl = runModeConfiguration.underlying.getString(("urls.registerScheme"))
  lazy val listOfSchemesUrl: String = s"${baseUrl("pensions-scheme")}${runModeConfiguration.underlying.getString("urls.listOfSchemes")}"
  lazy val inviteUrl: String = s"${baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.invite")}"
  lazy val minimalPsaDetailsUrl: String = s"${baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.minimalPsaDetails")}"
  lazy val acceptInvitationUrl = s"${baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.acceptInvite")}"
  lazy val schemeDetailsUrl: String = s"${baseUrl("pensions-scheme")}${runModeConfiguration.underlying.getString("urls.schemeDetails")}"
  lazy val viewSchemeDetailsUrl: String = runModeConfiguration.underlying.getString("urls.viewSchemeDetails")
  lazy val subscriptionDetailsUrl: String = s"${baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.subscriptionDetails")}"
  lazy val removePsaUrl : String = s"${baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.removePsa")}"

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage: String => Call = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val daysDataSaved: Int = loadConfig("daysDataSaved").toInt
  lazy val invitationExpiryDays: Int = loadConfig("invitationExpiryDays").toInt
  lazy val earliestDatePsaRemoval: LocalDate = new LocalDate(loadConfig("earliestDatePsaRemoval"))

  lazy val locationCanonicalList = loadConfig("location.canonical.list")
  lazy val locationCanonicalListEUAndEEA: String = loadConfig("location.canonical.list.EUAndEEA")
  lazy val addressLookUp = baseUrl("address-lookup")

  lazy val workPackageTwoEnabled: Boolean = runModeConfiguration.getBoolean("features.work-package-two-enabled").getOrElse(false)
}
