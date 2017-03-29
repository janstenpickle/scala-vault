logLevel := Level.Warn

// publishing and resolving bintray packages
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

// measure code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

// measure code style
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

// check dependencies
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")
