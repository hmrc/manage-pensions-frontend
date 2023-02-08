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

package config

import java.time.LocalDate
import com.google.inject.{Inject, Singleton}
import controllers.routes
import play.api.{Configuration, Mode, Environment}
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import models.ReportTechnicalIssue

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
  val reportTechnicalIssues = ReportTechnicalIssue(serviceId = "PODS", baseUrl = Some(reportAProblemNonJSUrl))
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

  def featureToggleUrl(toggle:String) : String =
    s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying.getString("urls.featureToggle").format(toggle)}"

  def aftFeatureToggleUrl(toggle: String): String =
    s"$aftUrl${runModeConfiguration.underlying.getString("urls.featureToggle").format(toggle)}"

  lazy val authUrl: String = servicesConfig.baseUrl("auth")
  lazy val pensionsSchemeUrl: String = servicesConfig.baseUrl("pensions-scheme")
  lazy val pensionAdminUrl: String = servicesConfig.baseUrl("pension-administrator")
  lazy val aftUrl: String = servicesConfig.baseUrl("pension-scheme-accounting-for-tax")
  lazy val practitionerUrl: String = servicesConfig.baseUrl("pension-practitioner")
  lazy val managePensionsUrl: String = servicesConfig.baseUrl("manage-pensions-frontend")

  lazy val loginUrl: String = loadConfig("urls.login")
  lazy val loginContinueUrl: String = loadConfig("urls.loginContinue")
  lazy val loginToListSchemesUrl: String = loadConfig("urls.loginToListSchemes")
  lazy val loginToListSchemesPspUrl: String = loadConfig("urls.pspLoginToListSchemes")
  lazy val serviceSignOut: String = loadConfig("urls.logout")
  lazy val registerSchemeAdministratorUrl: String = loadConfig("urls.registerSchemeAdministrator")
  lazy val registerSchemePractitionerUrl: String = loadConfig("urls.registerSchemePractitioner")
  lazy val recoverCredentialsPSAUrl: String = loadConfig("urls.recoverCredentialsPSA")
  lazy val recoverCredentialsPSPUrl: String = loadConfig("urls.recoverCredentialsPSP")
  lazy val pensionAdministratorGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.pensionAdministratorGovUkLink")
  lazy val pensionPractitionerGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.pensionPractitionerGovUkLink")
  lazy val pensionSchemesGuideMandatoryOnlineFilingGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.pensionSchemesGuideMandatoryOnlineFilingGovUkLink")
  lazy val guidanceStartPageGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.guidanceStartPageGovUkLink")
  lazy val pensionSchemesInvitationGuideGovUkPractitionerDeauthLink: String = runModeConfiguration.underlying
    .getString("urls.pensionSchemesInvitationGuideGovUkPractitionerDeauthLink")
  lazy val pensionSchemesInvitationGuideGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.pensionSchemesInvitationGuideGovUkLink")
  lazy val pensionSchemesAddToSchemeGuideGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.pensionSchemesAddToSchemeGuideGovUkLink")
   lazy val authorisePractitionerGuideGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.authorisePractitionerGuidance")
  lazy val registerSchemeGuideGovUkLink: String = runModeConfiguration.underlying
    .getString("urls.registerSchemeGuidance")
  lazy val pspUpdateDetailsTPSSLink: String = runModeConfiguration.underlying
    .getString("urls.pspUpdateDetailsTPSS")


  lazy val govUkLink: String = runModeConfiguration.underlying.getString("urls.govUkLink")
  lazy val continueSchemeUrl = s"${loadConfig("urls.continueSchemeRegistration")}"
  lazy val userResearchUrl: String = runModeConfiguration.underlying.getString("urls.userResearch")
  lazy val pensionSchemeOnlineServiceUrl: String = loadConfig("urls.pensionSchemeOnlineService")
  lazy val tpssWelcomeUrl: String = loadConfig("urls.tpssWelcome")
  lazy val tpssInitialQuestionsUrl: String = loadConfig("urls.tpssInitialQuestions")
  lazy val registeredPsaDetailsUrl: String = loadConfig("urls.psaDetails")
  lazy val psaDeregisterUrl: String = loadConfig("urls.psaDeregister")
  lazy val languageTranslationEnabled: Boolean = runModeConfiguration.get[Boolean]("features.welsh-translation")
  lazy val registerSchemeUrl: String = runModeConfiguration.underlying.getString("urls.registerScheme")
  lazy val listOfSchemesUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.listOfSchemes")}"
  lazy val inviteUrl: String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.invite")}"
  lazy val minimalPsaDetailsUrl: String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.minimalPsaDetails")}"
  lazy val acceptInvitationUrl = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.acceptInvite")}"
  lazy val schemeDetailsUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.schemeDetails")}"
  lazy val pspSchemeDetailsUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.pspSchemeDetails")}"
  lazy val pspTaskListUrl: String = runModeConfiguration.underlying.getString("urls.pspTaskList")
  lazy val viewSchemeDetailsUrl: String = runModeConfiguration.underlying.getString("urls.viewSchemeDetails")
  lazy val subscriptionDetailsUrl: String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.subscriptionDetails")}"
  lazy val removePsaUrl : String = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.removePsa")}"
  lazy val updateSchemeDetailsUrl: String = s"${servicesConfig.baseUrl("pensions-scheme")}${runModeConfiguration.underlying
    .getString("urls.updateSchemeDetails")}"

  lazy val aftPartialHtmlUrl: String = s"${servicesConfig.baseUrl("aft-frontend")}${runModeConfiguration.underlying
    .getString("urls.aftPartialHtml")}"
  lazy val pspSchemeDashboardCardsUrl: String = s"${servicesConfig.baseUrl("aft-frontend")}${runModeConfiguration.underlying
    .getString("urls.pspSchemeDashboardCardsUrl")}"
  lazy val paymentsAndChargesPartialHtmlUrl: String = s"${servicesConfig.baseUrl("aft-frontend")}${runModeConfiguration.underlying
    .getString("urls.paymentsAndChargesPartialHtml")}"

  lazy val schemeUrlsPartialHtmlUrl: String = s"${servicesConfig.baseUrl("scheme-frontend")}${runModeConfiguration.underlying
    .getString("urls.schemeUrlsPartialHtml")}"
  lazy val penaltiesUrlPartialHtmlUrl: String = s"${servicesConfig.baseUrl("aft-frontend")}${runModeConfiguration.underlying
    .getString("urls.penaltiesPartialHtml")}"
  lazy val eventReportingUrlPartialHtmlUrl: String = s"${servicesConfig.baseUrl("pension-scheme-event-reporting-frontend")}${runModeConfiguration.underlying
    .getString("urls.eventReportingPartialHtml")}"
  lazy val migrationUrlsPartialHtmlUrl: String = s"${servicesConfig.baseUrl("migration-frontend")}${runModeConfiguration.underlying
    .getString("urls.migrationUrlsPartialHtml")}"

  lazy val migrationListOfSchemesUrl: String = loadConfig("urls.migrationListOfSchemes")
  lazy val psaOverviewUrl: String = loadConfig("urls.psaOverview")
  lazy val pspDashboardUrl: String = loadConfig("urls.pspDashboard")

  lazy val authorisePspUrl = s"${servicesConfig.baseUrl("pension-practitioner")}${runModeConfiguration.underlying
    .getString("urls.authorisePsp")}"
  lazy val deAuthorisePspUrl = s"${servicesConfig.baseUrl("pension-practitioner")}${runModeConfiguration.underlying
    .getString("urls.deAuthorisePsp")}"

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

  lazy val contactHmrcUrl: String = runModeConfiguration.get[String]("urls.contactHmrc")
  lazy val submitEventReportGovUkLink: String = runModeConfiguration.get[String]("urls.guidanceToSubmitEventReportPageGovUkLink")
  lazy val updateClientReferenceUrl = s"${servicesConfig.baseUrl("pension-administrator")}${runModeConfiguration.underlying
    .getString("urls.updateClientReference")}"
}
