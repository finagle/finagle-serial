import UnidocKeys._
import scoverage.ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages

lazy val commonSettings = Seq(
  organization := "io.github.finagle",
  version := "0.0.1",
  scalaVersion := "2.11.6",
  crossScalaVersions := Seq("2.10.5", "2.11.6"),
  libraryDependencies ++= Seq(
    "com.twitter" %% "finagle-mux" % "6.25.0"
  ) ++ testDependencies.map(_ % "test"),
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  resolvers += "Twitter's Repository" at "http://maven.twttr.com/",
  parallelExecution in Test := false
)

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "2.2.4",
  "org.scalacheck" %% "scalacheck" % "1.12.2"
)

lazy val root = project.in(file("."))
  .settings(moduleName := "finagle-serial")
  .settings(commonSettings ++ publishSettings)
  .settings(unidocSettings ++ site.settings ++ ghpages.settings)
  .settings(
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmark),
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "docs"),
    git.remoteRepo := "git@github.com:finagle/finagle-serial.git"
  )
  .aggregate(core, test, scodec, benchmark)

lazy val core = project
  .settings(moduleName := "finagle-serial-core")
  .settings(commonSettings ++ publishSettings)
  .disablePlugins(CoverallsPlugin)

lazy val test = project
  .settings(moduleName := "finagle-serial-test")
  .settings(commonSettings ++ publishSettings)
  .settings(libraryDependencies ++= testDependencies)
  .settings(coverageExcludedPackages := "io\\.github\\.finagle\\.serial\\.test\\..*")
  .dependsOn(core)
  .disablePlugins(CoverallsPlugin)

lazy val scodecSettings = Seq(
  libraryDependencies += "org.scodec" %% "scodec-core" % "1.7.1",
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
  .settings(commonSettings ++ publishSettings ++ scodecSettings ++ Defaults.itSettings)
  .dependsOn(core, test % "it")
  .disablePlugins(CoverallsPlugin)

lazy val benchmark = project
  .settings(moduleName := "finagle-serial-benchmark")
  .settings(commonSettings ++ publishSettings ++ scodecSettings ++ jmhSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-thriftmux" % "6.25.0",
      "com.twitter" %% "scrooge-core" % "3.17.0"
    )
  )
  .settings(com.twitter.scrooge.ScroogeSBT.newSettings: _*)
  .settings(coverageExcludedPackages := "i\\.g\\.f\\.s\\..*")
  .dependsOn(core, scodec)
  .disablePlugins(CoverallsPlugin)

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
  autoAPIMappings := true,
  apiURL := Some(url("https://finagle.github.io/finagle-serial/docs/")),
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
