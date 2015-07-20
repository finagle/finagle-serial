resolvers ++= Seq(
  "jgit-repo" at "http://download.eclipse.org/jgit/maven",
  Classpaths.typesafeReleases,
  Classpaths.sbtPluginReleases
)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.2")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.4")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.1.8")

// Used only in the benchmark project
addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "3.16.3")
