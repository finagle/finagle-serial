resolvers ++= Seq(
  Classpaths.typesafeReleases,
  Classpaths.sbtPluginReleases
)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.1")

// Used only in the benchmark project
addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "3.14.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.1.8")
