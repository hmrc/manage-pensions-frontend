/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.i18n.Lang
import play.api.mvc.Call
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.duration.Duration

@Singleton
class FrontendAppConfig @Inject()(runModeConfiguration: Configuration, environment: Environment, servicesConfig: ServicesConfig) {

  def localFriendlyUrl(uri:String):String = loadConfig("host") + uri
  def urlInThisService(uri:String):String = servicesConfig.baseUrl("manage-pensions-frontend") + uri

  protected def mode: Mode = environment.mode

  private def getConfigString(key: String) = servicesConfig.getConfString(key,
    throw new Exception(s"Could not find config '$key'"))

  private def loadConfig(key: String): String = runModeConfiguration.get[String](key)


  lazy val appName: String = runModeConfiguration.underlying.getString("appName")

  val reportAProblemNonJSUrl = getConfigString("contact-frontend.report-problem-url.non-js")
  val betaFeedbackUnauthenticatedUrl = getConfigString("contact-frontend.beta-feedback-url.unauthenticated")
  def pspAuthEmailCallback(encryptedPsaId: String, encryptedPspId: String, encryptedPstr: String, encryptedEmail: String) =
    s"$practitionerUrl${runModeConfiguration.get[String](path = "urls.pspAuthEmailCallback")
      .format(encryptedPsaId, encryptedPspId, encryptedPstr, encryptedEmail)}"

  def pspDeauthEmailCallback(encryptedPsaId: String, encryptedPspId: String, encryptedPstr: String, encryptedEmail: String) =
    s"$practitionerUrl${runModeConfiguration.get[String](path = "urls.pspDeauthEmailCallback")
      .format(encryptedPsaId, encryptedPspId, encryptedPstr, encryptedEmail)}"

  def pspSelfDeauthEmailCallback(encryptedPspId: String, encryptedPstr: String, encryptedEmail: String) =
    s"$practitionerUrl${runModeConfiguration.get[String](path = "urls.pspSelfDeauthEmailCallback")
      .format(encryptedPspId, encryptedPstr, encryptedEmail)}"

  lazy val pensionsSchemeUrl: String = servicesConfig.baseUrl("pensions-scheme")
  lazy val pensionsSchemeReturnUrl: String = servicesConfig.baseUrl("pensions-scheme-return")
  lazy val pensionAdminUrl: String = servicesConfig.baseUrl("pension-administrator")
  lazy val practitionerUrl: String = servicesConfig.baseUrl("pension-practitioner")

  lazy val loginUrl: String = loadConfig("urls.login")
  lazy val loginContinueUrl: String = loadConfig("urls.loginContinue")
  lazy val loginToListSchemesUrl: String = loadConfig("urls.loginToListSchemes")
  lazy val loginToListSchemesPspUrl: String = loadConfig("urls.pspLoginToListSchemes")
  lazy val serviceSignOut: String = loadConfig("urls.logout")
  lazy val registerSchemeAdministratorUrl: String = loadConfig("urls.registerSchemeAdministrator")
  lazy val registerSchemePractitionerUrl: String = loadConfig("urls.registerSchemePractitioner")
  lazy val tpssEnrolmentRecoveryURL: String = loadConfig("urls.tpssEnrolmentRecovery")
  lazy val recoverCredentialsPSAUrl: String = loadConfig("urls.recoverCredentialsPSA")
  lazy val recoverCredentialsPSPUrl: String = loadConfig("urls.recoverCredentialsPSP")
  lazy val pensionAdministratorGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.pensionAdministratorGovUkLink")
  lazy val pensionPractitionerGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.pensionPractitionerGovUkLink")

  lazy val pensionSchemeOnlineServiceUrl: String = loadConfig("urls.pensionSchemeOnlineService")
  lazy val tpssWelcomeUrl: String = loadConfig("urls.tpssWelcome")
  lazy val registeredPsaDetailsUrl: String = loadConfig("urls.psaDetails")
  lazy val psaDeregisterUrl: String = loadConfig("urls.psaDeregister")
  lazy val listOfSchemesUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.listOfSchemes")}"
  lazy val inviteUrl: String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.invite")}"
  lazy val minimalPsaDetailsUrl: String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.minimalPsaDetails")}"
  lazy val emailDetailsUrl: String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.emailDetails")}"
  lazy val acceptInvitationUrl = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.acceptInvite")}"
  lazy val schemeDetailsUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.schemeDetails")}"
  lazy val psaInvitationInfoUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.psaInvitationInfo")}"
  lazy val isSchemeAssociatedUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.isSchemeAssociated")}"
  lazy val pspSchemeDetailsUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.pspSchemeDetails")}"
  lazy val pspTaskListUrl: String = runModeConfiguration.underlying.getString("urls.pspTaskList")
  lazy val viewSchemeDetailsUrl: String = runModeConfiguration.underlying.getString("urls.viewSchemeDetails")

  lazy val removePsaUrl : String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.removePsa")}"
  lazy val updateSchemeDetailsUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.updateSchemeDetails")}"

  lazy val aftPartialHtmlUrl: String = s"${servicesConfig.baseUrl("aft-frontend")}${runModeConfiguration.underlying
    .getString("urls.aftPartialHtml")}"


  lazy val aftOverviewHtmlUrl: String = runModeConfiguration.underlying
    .getString("urls.aftOverviewPartialLink")

  lazy val eventReportingOverviewHtmlUrl: String = runModeConfiguration.underlying
    .getString("urls.eventReportingOverviewPartialLink")

  lazy val psrOverviewUrl: String = runModeConfiguration.underlying.getString("urls.psrOverviewUrl")
  lazy val qropsOverviewUrl: String = runModeConfiguration.underlying.getString("urls.qropsOverviewUrl")
  lazy val enableQROPSUrl: Boolean = runModeConfiguration.getOptional[Boolean]("features.enableQROPSUrl").getOrElse(false)

  val ifsTimeout: Duration = runModeConfiguration.get[Duration]("ifs.timeout")

  lazy val finInfoPartialHtmlUrl: String = s"${servicesConfig.baseUrl("aft-frontend")}${runModeConfiguration.underlying
    .getString("urls.finInfoPartialHtml")}"
  lazy val eventReportingPartialHtmlUrl: String = s"${servicesConfig.baseUrl("pension-scheme-event-reporting-frontend")}${runModeConfiguration.underlying
    .getString("urls.eventReportingPartialHtml")}"
  lazy val pspSchemeDashboardCardsUrl: String = s"${servicesConfig.baseUrl("aft-frontend")}${runModeConfiguration.underlying
    .getString("urls.pspSchemeDashboardCardsUrl")}"
  lazy val paymentsAndChargesPartialHtmlUrl: String = s"${servicesConfig.baseUrl("aft-frontend")}${runModeConfiguration.underlying
    .getString("urls.paymentsAndChargesPartialHtml")}"

  lazy val schemeUrlsPartialHtmlUrl: String = s"${servicesConfig.baseUrl("scheme-frontend")}${runModeConfiguration.underlying
    .getString("urls.schemeUrlsPartialHtml")}"
  lazy val penaltiesUrlPartialHtmlUrl: String = s"${servicesConfig.baseUrl("aft-frontend")}${runModeConfiguration.underlying
    .getString("urls.penaltiesPartialHtml")}"
  lazy val migrationUrlsPartialHtmlUrl: String = s"${servicesConfig.baseUrl("migration-frontend")}${runModeConfiguration.underlying
    .getString("urls.migrationUrlsPartialHtml")}"

  lazy val migrationListOfSchemesUrl: String = loadConfig("urls.migrationListOfSchemes")
  lazy val psaOverviewUrl: String = loadConfig("urls.psaOverview")
  lazy val pspDashboardUrl: String = loadConfig("urls.pspDashboard")

  def authorisePspUrl(srn: String): String = s"${servicesConfig.baseUrl("pension-practitioner")}${runModeConfiguration.underlying
    .getString("urls.authorisePsp")}"
    .format(srn)
  def deAuthorisePspUrl(srn: String): String = s"${servicesConfig.baseUrl("pension-practitioner")}${runModeConfiguration.underlying
    .getString("urls.deAuthorisePsp")}"
    .format(srn)

  def deAuthorisePspSelfUrl(srn: String): String = s"${servicesConfig.baseUrl("pension-practitioner")}${runModeConfiguration.underlying
    .getString("urls.deAuthorisePspSelf")}"
    .format(srn)

  lazy val pspDetailsUrl: String = loadConfig("urls.pspDetails")
  lazy val psaUpdateContactDetailsUrl: String = loadConfig("urls.psaUpdateContactDetails")
  lazy val pspUpdateContactDetailsUrl: String = loadConfig("urls.pspUpdateContactDetails")
  lazy val pspDeregisterCompanyUrl: String = loadConfig("urls.pspDeregisterCompany")
  lazy val pspDeregisterIndividualUrl: String = loadConfig("urls.pspDeregisterIndividual")

  lazy val baseUrlEmail: String = servicesConfig.baseUrl("email")
  lazy val emailUrl: String = s"$baseUrlEmail${runModeConfiguration.underlying.getString("urls.email")}"

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy"))

  def routeToSwitchLanguage: String => Call = (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val emailPsaDeauthorisePspTemplateId: String = runModeConfiguration.get[String]("email.psaDeauthorisePspTemplateId")

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

  lazy val contactHmrcUrl: String = runModeConfiguration.get[String]("urls.contactHmrc")
  lazy val submitEventReportGovUkLink: String = runModeConfiguration.get[String]("urls.guidanceToSubmitEventReportPageGovUkLink")
  lazy val updateClientReferenceUrl = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.updateClientReference")}"

  lazy val urBannerEmail: String = loadConfig("urBannerEmail")

  lazy val psaSchemeDashboardUrl: String = loadConfig("urls.psaSchemeDashboard")
  lazy val pspSchemeDashboardUrl: String = loadConfig("urls.pspSchemeDashboard")

  lazy val hideAftTile: Boolean = runModeConfiguration.get[Boolean]("hideAftTile")
  lazy val showPsrLink: Boolean = runModeConfiguration.get[Boolean]("show-psr-link")

  lazy val checkMembersProtectionsEnhancementsUrl: String = runModeConfiguration.underlying.getString("urls.checkMembersProtectionsEnhancements")
  lazy val enableMembersProtectionsEnhancements: Boolean = runModeConfiguration.getOptional[Boolean]("features.enableMPELink").getOrElse(false)
  lazy val forceServiceNavigation: Boolean = runModeConfiguration.getOptional[Boolean]("play-frontend-hmrc.forceServiceNavigation").getOrElse(false)
}
