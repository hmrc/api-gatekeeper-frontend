import scoverage.ScoverageKeys

object ScoverageSettings {
  def apply() = Seq(
    ScoverageKeys.coverageMinimumStmtTotal := 85.5,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageExcludedPackages := Seq(
      "<empty>",
      """.*\.controllers\.binders""",
      """uk\.gov\.hmrc\.BuildInfo""" ,
      """.*\.Routes""" ,
      """.*\.RoutesPrefix""" ,
      """.*\.Reverse[^.]*""",
      """uk\.gov\.hmrc\.apiplatform\.modules\.common\..*""",
      """uk\.gov\.hmrc\.apiplatform\.modules\.commands\.applications\.domain\.models\..*"""
    ).mkString(";")
  )
}
