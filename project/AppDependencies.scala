import sbt.*

object AppDependencies {

  private val playVersion = "play-30"
  private val bootstrapVersion = "10.2.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% s"play-conditional-form-mapping-$playVersion" % "3.3.0",
    "uk.gov.hmrc"                   %% s"bootstrap-frontend-$playVersion"            % bootstrapVersion,
    "uk.gov.hmrc"                   %% s"domain-$playVersion"                        % "13.0.0",
    "uk.gov.hmrc"                   %% s"play-partials-$playVersion"                 % "10.2.0",
    "uk.gov.hmrc"                   %% s"play-frontend-hmrc-$playVersion"            % "12.17.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"                        % "2.20.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "org.scalatestplus"             %% "scalacheck-1-17"               % "3.2.18.0",
    "com.vladsch.flexmark"          %  "flexmark-all"                  % "0.64.8",
    "org.jsoup"                     %  "jsoup"                         % "1.21.2",
    "io.github.wolfendale"          %% "scalacheck-gen-regexp"         % "1.1.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
