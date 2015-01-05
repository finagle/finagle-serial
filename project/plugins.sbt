resolvers ++= Seq(
  Classpaths.typesafeReleases,
  Classpaths.sbtPluginReleases
)

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")
