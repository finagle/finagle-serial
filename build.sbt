lazy val commonSettings = Seq(
  organization := "io.github.finagle",
  version := "0.0.1",
  scalaVersion := "2.11.5",
  crossScalaVersions := Seq("2.10.4", "2.11.5"),
  libraryDependencies ++= Seq(
    "com.twitter" %% "finagle-mux" % "6.24.0"
  ) ++ testDependencies.map(_ % "test"),
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  resolvers += "Twitter's Repository" at "http://maven.twttr.com/",
  parallelExecution in Test := false
)

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "2.2.3",
  "org.scalacheck" %% "scalacheck" % "1.12.1"
)

lazy val root = project.in(file("."))
  .settings(moduleName := "finagle-serial")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(unidocSettings: _*)
  .settings {
    import UnidocKeys._

    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmark)
  }
  .aggregate(core, test, scodec, benchmark)


lazy val core = project
  .settings(moduleName := "finagle-serial-core")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)

lazy val test = project
  .settings(moduleName := "finagle-serial-test")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(libraryDependencies ++= testDependencies)
  .dependsOn(core)

lazy val scodecSettings = Seq(
  resolvers += Resolver.sonatypeRepo("snapshots"),
  libraryDependencies += "org.scodec" %% "scodec-core" % "1.8.0-SNAPSHOT",
  // This is necessary for 2.10 because of Scodec's Shapeless dependency.
  libraryDependencies ++= (
    if (scalaBinaryVersion.value.startsWith("2.10")) Seq(
      compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
    ) else Nil
  )
)

lazy val scodec = project
  .settings(moduleName := "finagle-serial-scodec")
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(scodecSettings: _*)
  .dependsOn(core, test % "it")

lazy val benchmark = project
  .settings(moduleName := "finagle-serial-benchmark")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(scodecSettings: _*)
  .settings(jmhSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-thriftmux" % "6.24.0",
      "com.twitter" %% "scrooge-core" % "3.17.0"
    )
  ).settings(com.twitter.scrooge.ScroogeSBT.newSettings: _*)
  .dependsOn(core, scodec)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  useGpg := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/finagle/finagle-serial")),
  pomExtra := (
    <scm>
      <url>git://github.com/finagle/finagle-serial.git</url>
      <connection>scm:git://github.com/finagle/finagle-serial.git</connection>
    </scm>
    <developers>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
      </developer>
      <developer>
        <id>vkostyukov</id>
        <name>Vladimir Kostyukov</name>
      </developer>
    </developers>
  )
)
