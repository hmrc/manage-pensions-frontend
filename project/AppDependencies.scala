import sbt._

object AppDependencies {
  private val bootstrapVersion = "9.13.0"
  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping-play-30" % "2.0.0",
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc"                   %% "domain-play-30"                % "10.0.0",
    "uk.gov.hmrc"                   %% "play-partials-play-30"         % "9.1.0",
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"    % "11.13.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"          % "2.19.1"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-30"        % bootstrapVersion,
    "org.scalatest"                 %% "scalatest"                     % "3.2.19",
    "org.scalatestplus"             %% "scalacheck-1-17"               % "3.2.18.0",
    "org.scalatestplus.play"        %% "scalatestplus-play"            % "7.0.1",
    "org.scalatestplus"             %% "mockito-4-6"                   % "3.2.15.0",
    "org.jsoup"                     %  "jsoup"                         % "1.21.1",
    "io.github.wolfendale"          %% "scalacheck-gen-regexp"         % "1.1.0",
    "com.vladsch.flexmark"          % "flexmark-all"                   % "0.64.8"
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}
