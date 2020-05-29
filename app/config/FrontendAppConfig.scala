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

package config

import java.time.LocalDate

import com.google.inject.{Inject, Singleton}
import controllers.routes
import play.api.Mode
import play.api.i18n.Lang
import play.api.mvc.Call
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject()(runModeConfiguration: Configuration, environment: Environment, servicesConfig: ServicesConfig) {

  protected def mode: Mode = environment.mode

  private def baseUrl(serviceName: String) = {
    val protocol = runModeConfiguration.getOptional[String](s"microservice.services.$serviceName.protocol").getOrElse("http")
    val host = runModeConfiguration.get[String](s"microservice.services.$serviceName.host")
    val port = runModeConfiguration.get[String](s"microservice.services.$serviceName.port")
    s"$protocol://$host:$port"
  }

  private def getConfigString(key: String) = servicesConfig.getConfString(key, throw new Exception(s"Could not find config '$key'"))

  private def loadConfig(key: String): String = runModeConfiguration.get[String](key)

  lazy val contactHost = baseUrl("contact-frontend")

  lazy val appName: String = runModeConfiguration.underlying.getString("appName")
  lazy val googleTagManagerIdAvailable: Boolean = runModeConfiguration.underlying.getBoolean(s"google-tag-manager.id-available")
  lazy val googleTagManagerId: String = loadConfig(s"google-tag-manager.id")

  val reportAProblemPartialUrl = getConfigString("contact-frontend.report-problem-url.with-js")
  val reportAProblemNonJSUrl = getConfigString("contact-frontend.report-problem-url.non-js")
  val betaFeedbackUrl = getConfigString("contact-frontend.beta-feedback-url.authenticated")
  val betaFeedbackUnauthenticatedUrl = getConfigString("contact-frontend.beta-feedback-url.unauthenticated")

  lazy val authUrl: String = servicesConfig.baseUrl("auth")
  lazy val pensionsSchemeUrl: String = servicesConfig.baseUrl("pensions-scheme")
  lazy val pensionAdminUrl: String = servicesConfig.baseUrl("pension-administrator")
  lazy val aftUrl: String = servicesConfig.baseUrl("pension-scheme-accounting-for-tax")

  lazy val timeout: String = loadConfig("session._timeoutSeconds")
  lazy val countdown: String = loadConfig("session._CountdownInSeconds")

  lazy val loginUrl: String = loadConfig("urls.login")
  lazy val loginContinueUrl: String = loadConfig("urls.loginContinue")
  lazy val serviceSignOut: String = loadConfig("urls.logout")
  lazy val registerSchemeAdministratorUrl: String = loadConfig("urls.registerSchemeAdministrator")
  lazy val pensionAdministratorGovUkLink: String = runModeConfiguration.underlying.getString("urls.pensionAdministratorGovUkLink")
  lazy val pensionPractitionerGovUkLink: String = runModeConfiguration.underlying.getString("urls.pensionPractitionerGovUkLink")
  lazy val govUkLink: String = runModeConfiguration.underlying.getString("urls.govUkLink")
  lazy val continueSchemeUrl = s"${loadConfig("urls.continueSchemeRegistration")}"
  lazy val userResearchUrl: String = runModeConfiguration.underlying.getString("urls.userResearch")
  lazy val pensionSchemeOnlineServiceUrl: String = loadConfig("urls.pensionSchemeOnlineService")
  lazy val registeredPsaDetailsUrl: String = loadConfig("urls.psaDetails")
  lazy val languageTranslationEnabled: Boolean = runModeConfiguration.get[Boolean]("features.welsh-translation")
  lazy val registerSchemeUrl: String = runModeConfiguration.underlying.getString("urls.registerScheme")
  lazy val listOfSchemesUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying.getString("urls.listOfSchemes")}"
  lazy val inviteUrl: String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.invite")}"
  lazy val minimalPsaDetailsUrl: String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.minimalPsaDetails")}"
  lazy val acceptInvitationUrl = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.acceptInvite")}"
  lazy val schemeDetailsUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying.getString("urls.schemeDetails")}"
  lazy val viewSchemeDetailsUrl: String = runModeConfiguration.underlying.getString("urls.viewSchemeDetails")
  lazy val subscriptionDetailsUrl: String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.subscriptionDetails")}"
  lazy val removePsaUrl : String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.removePsa")}"
  lazy val deregisterPsaUrl : String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.deregisterPsa")}"
  def canDeRegisterPsaUrl(psaId: String): String = s"${servicesConfig.baseUrl("pension-administrator") +
    runModeConfiguration.underlying.getString("urls.canDeRegister").format(psaId)}"
  lazy val taxDeEnrolmentUrl: String = servicesConfig.baseUrl("tax-enrolments") +runModeConfiguration.underlying.getString("urls.tax-de-enrolment")
  lazy val updateSchemeDetailsUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying.getString("urls.updateSchemeDetails")}"

  lazy val aftPartialHtmlUrl: String = loadConfig("urls.aftPartialHtml")
  lazy val aftLoginUrl: String = loadConfig("urls.aftLoginLink")
  lazy val aftSummaryPageUrl: String = loadConfig("urls.aftSummaryPageLink")
  lazy val aftSummaryPageNoVersionUrl: String = loadConfig("urls.aftSummaryPageNoVersionLink")
  lazy val aftReturnHistoryUrl: String = loadConfig("urls.aftReturnHistoryLink")
  lazy val aftContinueReturnUrl: String = loadConfig("urls.aftContinueReturn")
  lazy val aftAmendUrl: String = loadConfig("urls.aftAmendLink")
  lazy val isAFTEnabled: Boolean = runModeConfiguration.underlying.getBoolean("features.aft-return-enabled")
  lazy val aftListOfVersions: String = s"${servicesConfig.baseUrl("pension-scheme-accounting-for-tax")}${runModeConfiguration.underlying.getString("urls.aftListOfVersions")}"
  lazy val aftOverviewUrl: String = s"${servicesConfig.baseUrl("pension-scheme-accounting-for-tax")}${runModeConfiguration.underlying.getString("urls.aftOverview")}"
  lazy val isAftNonZero: String = s"${servicesConfig.baseUrl("pension-scheme-accounting-for-tax")}${runModeConfiguration.underlying.getString("urls.isAftNonZero")}"


  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage: String => Call = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val daysDataSaved: Int = loadConfig("daysDataSaved").toInt
  lazy val invitationExpiryDays: Int = loadConfig("invitationExpiryDays").toInt
  lazy val earliestDatePsaRemoval: LocalDate = LocalDate.parse(loadConfig("earliestDatePsaRemoval"))

  lazy val locationCanonicalList: String = loadConfig("location.canonical.list")
  lazy val locationCanonicalListEUAndEEA: String = loadConfig("location.canonical.list.EUAndEEA")
  lazy val addressLookUp: String = servicesConfig.baseUrl("address-lookup")


  lazy val retryAttempts: Int = runModeConfiguration.get[Int]("retry.max.attempts")
  lazy val retryWaitMs: Int = runModeConfiguration.get[Int]("retry.initial.wait.ms")
  lazy val retryWaitFactor: Double = runModeConfiguration.get[Double]("retry.wait.factor")

  lazy val listSchemePagination: Int = runModeConfiguration.get[Int]("listSchemePagination")
  lazy val minimumSchemeSearchResults: Int = runModeConfiguration.get[Int]("minimumSchemeSearchResults")
  lazy val overviewApiEnablementDate: String = runModeConfiguration.get[String]("overviewApiEnablementDate")
  lazy val quarterStartDate: String = runModeConfiguration.get[String]("earliestStartDate")
  lazy val quarterEndDate: String = "2020-06-30"
  lazy val aftNoOfYearsDisplayed: Int = runModeConfiguration.get[Int]("aftNoOfYearsDisplayed")
}
