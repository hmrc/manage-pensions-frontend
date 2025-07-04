import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "manage-pensions-frontend"

lazy val root = (project in file("."))
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(inConfig(Test)(testSettings)*)
  .settings(majorVersion := 0)
  .settings(scalaVersion := "2.13.16")
  .settings(
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "models.SchemeReferenceNumber",
      "models.Mode",
      "models.CheckMode",
      "models.NormalMode"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
    ),
    PlayKeys.playDefaultPort := 8204,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*models.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*FrontendAuditConnector.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*MongoDiagnosticsConnector;.*controllers.testonly.*;.*target.*",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq("-feature"),
    scalacOptions ++= Seq("-Xmaxerrs", "500"),   // Set maximum errors to 500
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    update / evictionWarningOptions :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    // concatenate js
    Concat.groups := Seq(
      "javascripts/application.js" -> group(Seq(
        "javascripts/autocomplete/location-autocomplete.min.js",
        "javascripts/managepensionsfrontend.js"
      ))
    ),
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    // Added below code to replace plugin silencer version = "1.7.8"
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:src=html/.*:s",
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to operate in dev rather than only prod
    // Removed uglify due to node 20 compile issues.
    // Suspected cause minification of already minified location-autocomplete.min.js -Pavel Vjalicin
    Assets / pipelineStages := Seq(concat)
  )

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
  fork := true,
  parallelExecution := true,
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)