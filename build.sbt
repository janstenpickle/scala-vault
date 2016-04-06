import sbt.Keys._

name := "scala-vault"

lazy val scalazVersion = "7.2.1"
lazy val specs2Version = "3.7.2"

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.8",
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-ws" % "2.5.1",
    "org.scalaz" %% "scalaz-core" % scalazVersion,
    "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
    "org.specs2" %% "specs2-core" % specs2Version % "it,test",
    "org.specs2" %% "specs2-scalacheck" % specs2Version % "it,test",
    "org.specs2" %% "specs2-junit" % specs2Version % "it,test"
  ),
  scalacOptions in Test ++= Seq(
    "-Yrangepos",
    "-Xlint",
    "-deprecation",
    "-Xfatal-warnings"
  ),
  scalacOptions ++= Seq(
    "-Xlint",
    "-Xcheckinit",
    "-Xfatal-warnings",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:implicitConversions"),
  testOptions in IntegrationTest ++= Seq( Tests.Argument("junitxml"), Tests.Argument("console") ),
  unmanagedSourceDirectories in IntegrationTest += baseDirectory.value / "test" / "scala"
) ++ Defaults.itSettings

lazy val core = (project in file("core")).
  settings(commonSettings: _*).
  configs(IntegrationTest)
lazy val auth = (project in file("auth")).
  settings(commonSettings: _*).
  configs(IntegrationTest).
  dependsOn(core % "compile->compile;it->it", manage % "it->compile")
lazy val manage = (project in file("manage")).
  settings(commonSettings: _*).
  configs(IntegrationTest).
  dependsOn(core % "compile->compile;it->it,it->test")