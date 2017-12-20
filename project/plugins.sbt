logLevel := Level.Warn

// publishing and resolving bintray packages
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.2")

// measure code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// measure code style
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// check dependencies
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.3")
