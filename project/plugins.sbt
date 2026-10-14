addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.8.7")
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.8.7")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")
// sbt-typelevel's cross-project support loads the Native plugin on JVM/JS builds too.
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.12")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")
