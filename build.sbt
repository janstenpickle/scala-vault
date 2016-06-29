import sbt.Keys._

name := "vault"

lazy val uscalaVersion = "0.2.2"
lazy val specs2Version = "3.7.2"
lazy val circeVersion = "0.4.1"

val pomInfo = (
  <url>https://github.com/janstenpickle/scala-vault</url>
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
  version := "0.3.0",
  scalaVersion := "2.11.8",
  organization := "janstenpickle.vault",
  pomExtra := pomInfo,
  autoAPIMappings := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  bintrayReleaseOnPublish := false,
  licenses += ("MIT", url("https://github.com/janstenpickle/scala-vault/blob/master/LICENSE")),
  resolvers ++= Seq(Resolver.sonatypeRepo("releases"), "Bintray jcenter" at "https://jcenter.bintray.com/"),
  libraryDependencies ++= Seq(
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
    "org.uscala" %% "uscala-result" % uscalaVersion,
    "org.uscala" %% "uscala-result-specs2" % uscalaVersion % "it,test",
    "org.specs2" %% "specs2-core" % specs2Version % "it,test",
    "org.specs2" %% "specs2-scalacheck" % specs2Version % "it,test",
    "org.specs2" %% "specs2-junit" % specs2Version % "it,test"
  ),
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion),
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
