import sbt._

object AppDependencies {

  import play.core.PlayVersion

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "logback-json-logger"           % "4.9.0",
    "uk.gov.hmrc"       %% "govuk-template"                % "5.60.0-play-26",
    "uk.gov.hmrc"       %% "play-health"                   % "3.16.0-play-26",
    "uk.gov.hmrc"       %% "play-ui"                       % "8.19.0-play-26",
    "uk.gov.hmrc"       %% "http-caching-client"           % "9.2.0-play-26",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.5.0-play-26",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-26"    % "3.2.0",
    "uk.gov.hmrc"       %% "play-language"                 % "4.5.0-play-26",
    "uk.gov.hmrc"       %% "domain"                        % "5.10.0-play-26",
    "uk.gov.hmrc"       %% "play-whitelist-filter"         % "3.4.0-play-26",
    "com.typesafe.play" %% "play-json-joda"                % "2.6.10",
    "uk.gov.hmrc"       %% "play-partials"                 % "7.1.0-play-26"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-frontend-play-26"  % "3.2.0" % Test classifier "tests",
    "uk.gov.hmrc"            %% "hmrctest"                    % "3.10.0-play-26",
    "org.scalatest"          %% "scalatest"                   % "3.0.8",
    "org.scalatestplus.play" %% "scalatestplus-play"          % "3.1.2",
    "org.scalacheck"         %% "scalacheck"                  % "1.14.0",
    "org.pegdown"            %  "pegdown"                     % "1.6.0",
    "org.jsoup"              %  "jsoup"                       % "1.12.1",
    "com.typesafe.play"      %% "play-test"                   % PlayVersion.current,
    "org.mockito"            %  "mockito-all"                 % "1.10.19",
    "com.github.tomakehurst" %  "wiremock-jre8"               % "2.21.0",
    "wolfendale"             %% "scalacheck-gen-regexp"       % "0.1.1"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
