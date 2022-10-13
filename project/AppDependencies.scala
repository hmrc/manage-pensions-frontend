import sbt._

object AppDependencies {

  import play.core.PlayVersion

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "logback-json-logger"           % "5.2.0",
    "uk.gov.hmrc"       %% "play-ui"                       % "9.11.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"           % "10.0.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.12.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % "7.7.0",
    "uk.gov.hmrc"       %% "domain"                        % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "play-partials"                 % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "3.32.0-play-28"

  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"                   % "3.0.9",
    "org.scalatestplus.play" %% "scalatestplus-play"          % "4.0.3",
    "org.scalacheck"         %% "scalacheck"                  % "1.14.0",
    "org.pegdown"            %  "pegdown"                     % "1.6.0",
    "org.jsoup"              %  "jsoup"                       % "1.12.1",
    "com.typesafe.play"      %% "play-test"                   % PlayVersion.current,
    "org.mockito"            %  "mockito-core"                 % "3.7.7",
    "com.github.tomakehurst" %  "wiremock-jre8"               % "2.26.0",
    "wolfendale"             %% "scalacheck-gen-regexp"       % "0.1.2"
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}
