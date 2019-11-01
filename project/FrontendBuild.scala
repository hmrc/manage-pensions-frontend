import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "manage-pensions-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "play-reactivemongo"            % "6.7.0",
    "uk.gov.hmrc" %% "logback-json-logger"           % "4.0.0",
    "uk.gov.hmrc" %% "govuk-template"                % "5.26.0-play-25",
    "uk.gov.hmrc" %% "play-health"                   % "3.14.0-play-25",
    "uk.gov.hmrc" %% "play-ui"                       % "8.3.0-play-25",
    "uk.gov.hmrc" %% "http-caching-client"           % "9.0.0-play-25",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
    "uk.gov.hmrc" %% "bootstrap-play-25"             % "5.1.0",
    "uk.gov.hmrc" %% "play-language"                 % "3.4.0",
    "uk.gov.hmrc" %% "domain"                        % "5.6.0-play-25",
    "uk.gov.hmrc" %% "play-whitelist-filter"         % "2.0.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc"            %% "hmrctest"              % "3.9.0-play-25" % scope,
        "org.scalatest"          %% "scalatest"             % "3.0.4" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play"    % "2.0.1" % scope,
        "org.scalacheck"         %% "scalacheck"            % "1.14.0" % scope,
        "org.pegdown"            %  "pegdown"               % "1.6.0" % scope,
        "org.jsoup"              %  "jsoup"                 % "1.11.3" % scope,
        "com.typesafe.play"      %% "play-test"             % PlayVersion.current % scope,
        "org.mockito"            %  "mockito-all"           % "1.10.19" % scope,
        "com.github.tomakehurst" %  "wiremock"              % "2.15.0" % scope,
        "wolfendale"             %% "scalacheck-gen-regexp" % "0.1.1" % scope

      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
