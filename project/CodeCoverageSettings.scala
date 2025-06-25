import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  val excludedPackages: Seq[String] = Seq(
    "<empty>",
    ".*Reverse.*",
    ".*filters.*",
    ".*handlers.*",
    ".*components.*",
    ".*models.*",
    ".*repositories.*",
    ".*BuildInfo.*",
    ".*javascript.*",
    ".*FrontendAuditConnector.*",
    ".*Routes.*",
    ".*GuiceInjector",
    ".*ControllerConfiguration",
    ".*LanguageSwitchController",
    ".*MongoDiagnosticsConnector",
    ".*testonly.*",
    ".*testOnly.*",
    ".*templates.*",
    ".*checkyouranswers.*",
    ".*target.*"
  )

  def apply(): Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageMinimumStmtTotal := 86,
    ScoverageKeys.coverageMinimumBranchTotal := 76,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageExcludedPackages:= excludedPackages.mkString(";")
  )
}
