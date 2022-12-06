import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "logback-json-logger"           % "5.2.0",
    "uk.gov.hmrc"                   %% "play-ui"                       % "9.11.0-play-28",
    "uk.gov.hmrc"                   %% "http-caching-client"           % "10.0.0-play-28",
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping" % "1.12.0-play-28",
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-28"    % "7.11.0",
    "uk.gov.hmrc"                   %% "domain"                        % "8.1.0-play-28",
    "uk.gov.hmrc"                   %% "play-partials"                 % "8.3.0-play-28",
    "uk.gov.hmrc"                   %% "play-frontend-hmrc"            % "3.32.0-play-28",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"          % "2.14.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"                 %% "scalatest"                     % "3.2.14",
    "org.scalatestplus"             %% "scalacheck-1-17"               % "3.2.14.0",
    "org.scalatestplus.play"        %% "scalatestplus-play"            % "5.1.0",
    "org.scalatestplus"             %% "mockito-4-6"                   % "3.2.14.0",
    "org.pegdown"                   %  "pegdown"                       % "1.6.0",
    "org.jsoup"                     %  "jsoup"                         % "1.15.3",
    "com.github.tomakehurst"        %  "wiremock-jre8"                 % "2.35.0",
    "io.github.wolfendale"          %% "scalacheck-gen-regexp"         % "1.0.0",
    "com.vladsch.flexmark"          % "flexmark-all"                   % "0.62.2"
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}
