import ReleaseTransformations._
import UnidocKeys._

lazy val buildSettings = Seq(
  organization := "io.github.finagle",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint"
)

lazy val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "com.twitter" %% "finagle-mux" % "6.39.0"
  ) ++ testDependencies.map(_ % "test"),
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Seq.empty
    }
  ),
  scalacOptions in (Compile, console) := compilerOptions,
  resolvers += "Twitter's Repository" at "https://maven.twttr.com/",
  parallelExecution in Test := false
)

lazy val testDependencies = Seq(
  "org.scalacheck" %% "scalacheck" % "1.13.4",
  "org.scalatest" %% "scalatest" % "3.0.0"
)

val docMappingsAPIDir = settingKey[String]("Name of subdirectory in site target directory for API docs")

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val root = project.in(file("."))
  .settings(moduleName := "finagle-serial")
  .settings(allSettings)
  .settings(unidocSettings ++ ghpages.settings)
  .settings(
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmark),
    docMappingsAPIDir := "docs",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), docMappingsAPIDir),
    git.remoteRepo := "git@github.com:finagle/finagle-serial.git"
  )
  .settings(
    initialCommands in console :=
      """
        |import com.twitter.finagle.Service
        |import com.twitter.util.Future
        |import io.github.finagle.serial.scodec.ScodecSerial
        |import scodec.Codec
        |import scodec.codecs._
      """.stripMargin
  )
  .aggregate(core, tests, scodec, benchmark)
  .dependsOn(core, scodec)

lazy val core = project
  .settings(moduleName := "finagle-serial-core")
  .settings(allSettings)

lazy val tests = project
  .settings(moduleName := "finagle-serial-tests")
  .settings(allSettings)
  .settings(libraryDependencies ++= testDependencies)
  .settings(coverageExcludedPackages := "io\\.github\\.finagle\\.serial\\.tests\\..*")
  .dependsOn(core)

lazy val scodecSettings = Seq(
  libraryDependencies += "org.scodec" %% "scodec-core" % "1.10.3"
)

lazy val scodec = project
  .settings(moduleName := "finagle-serial-scodec")
  .configs(IntegrationTest)
  .settings(allSettings ++ scodecSettings ++ Defaults.itSettings)
  .dependsOn(core, tests % "it")

lazy val benchmark = project
  .settings(moduleName := "finagle-serial-benchmark")
  .settings(allSettings ++ scodecSettings ++ noPublishSettings)
  .settings(coverageExcludedPackages := "io\\.github\\.finagle\\.serial\\.benchmark\\..*")
  .enablePlugins(JmhPlugin)
  .dependsOn(core, scodec, benchmarkThrift)

lazy val benchmarkThrift = project.in(file("benchmark-thrift"))
  .settings(moduleName := "finagle-serial-benchmark-thrift")
  .settings(allSettings ++ scodecSettings ++ noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-thriftmux" % "6.39.0",
      "com.twitter" %% "scrooge-core" % "4.11.0"
    )
  )
  .settings(coverageExcludedPackages := "io\\.github\\.finagle\\.serial\\.benchmark\\..*")
  .dependsOn(core, scodec)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/finagle/finagle-serial")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://finagle.github.io/finagle-serial/docs/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/finagle/finagle-serial"),
      "scm:git:git@github.com:finagle/finagle-serial.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
        <url>https://twitter.com/travisbrown</url>
      </developer>
      <developer>
        <id>vkostyukov</id>
        <name>Vladimir Kostyukov</name>
        <url>https://twitter.com/vkostyukov</url>
      </developer>
    </developers>
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq
