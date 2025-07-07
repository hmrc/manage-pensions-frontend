import play.sbt.routes.RoutesKeys
import sbt.Def
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "manage-pensions-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.7.1"
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-Xfatal-warnings",
  "-Wconf:src=routes/.*:s",
  "-Wconf:src=html/.*:s",
  "-Wconf:msg=Flag.*repeatedly:s", // Suppress repeated flag warnings
  "-Wconf:msg=-Wunused.set.to.all.redundantly:s", // Suppress repeated flag warnings
)

lazy val root = (project in file("."))
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(inConfig(Test)(testSettings) *)
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
    CodeCoverageSettings(),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    // concatenate js
    Concat.groups := Seq(
      "javascripts/application.js" -> group(Seq(
        "javascripts/autocomplete/location-autocomplete.min.js",
        "javascripts/managepensionsfrontend.js"
      ))
    ),
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
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