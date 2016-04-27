import sbt.Keys._

name := "vault"

lazy val scalazVersion = "7.2.1"
lazy val specs2Version = "3.7.2"

val pomInfo = (
  <url>https://github.com/intenthq/pucket</url>
  <licenses>
    <license>
      <name>The MIT License (MIT)</name>
      <url>https://github.com/janstenpickle/scala-vault/blob/master/LICENSE</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:janstenpickle/scala-vault.git</url>
    <connection>scm:git:git@github.com:janstenpickle/scala-vault.git</connection>
  </scm>
  <developers>
    <developer>
      <id>janstepickle</id>
      <name>Chris Jansen</name>
    </developer>
  </developers>
)

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := "2.11.8",
  organization := "janstenpickle.vault",
  pomExtra := pomInfo,
  autoAPIMappings := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  bintrayReleaseOnPublish := false,
  licenses += ("MIT", url("https://github.com/janstenpickle/scala-vault/blob/master/LICENSE")),
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
  settings(name := "vault-core").
  settings(commonSettings: _*).
  configs(IntegrationTest)
lazy val auth = (project in file("auth")).
  settings(name := "vault-auth").
  settings(commonSettings: _*).
  configs(IntegrationTest).
  dependsOn(core % "compile->compile;it->it", manage % "it->compile")
lazy val manage = (project in file("manage")).
  settings(name := "vault-manage").
  settings(commonSettings: _*).
  configs(IntegrationTest).
  dependsOn(core % "compile->compile;it->it,it->test")