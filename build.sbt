organization in ThisBuild := "com.yuiwai"
version in ThisBuild := "0.1.0"
scalaVersion in ThisBuild := "2.12.4"

lazy val core = (project in file("core"))
  .settings(
    name := "tokachi-core"
  )

lazy val example = (project in file("example"))
  .settings(
    name := "tokachi-example"
  )
  .dependsOn(core)
