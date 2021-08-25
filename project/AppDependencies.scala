import sbt._

object AppDependencies {

  import play.core.PlayVersion

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "logback-json-logger"           % "5.1.0",
    "uk.gov.hmrc"       %% "govuk-template"                % "5.69.0-play-28",
    "uk.gov.hmrc"       %% "play-ui"                       % "9.6.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"           % "9.5.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.9.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % "5.12.0",
    "uk.gov.hmrc"       %% "play-language"                 % "5.1.0-play-28",
    "uk.gov.hmrc"       %% "domain"                        % "6.2.0-play-28",
    "uk.gov.hmrc"       %% "play-partials"                 % "8.2.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"                   % "3.0.8",
    "org.scalatestplus.play" %% "scalatestplus-play"          % "4.0.2",
    "org.scalacheck"         %% "scalacheck"                  % "1.14.0",
    "org.pegdown"            %  "pegdown"                     % "1.6.0",
    "org.jsoup"              %  "jsoup"                       % "1.12.1",
    "com.typesafe.play"      %% "play-test"                   % PlayVersion.current,
    "org.mockito"            %  "mockito-core"                 % "3.7.7",
    "com.github.tomakehurst" %  "wiremock-jre8"               % "2.26.0",
    "wolfendale"             %% "scalacheck-gen-regexp"       % "0.1.1"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
