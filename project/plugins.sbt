resolvers ++= Seq(
  "Twitter's Repository" at "https://maven.twttr.com/",
  "jgit-repo" at "http://download.eclipse.org/jgit/maven",
  Classpaths.typesafeReleases,
  Classpaths.sbtPluginReleases
)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.7.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.2.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.3")

// Used only in the benchmark project
addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "4.1.0")
