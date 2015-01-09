lazy val commonSettings = Seq(
  organization := "io.github.finagle",
  version := "0.0.1",
  scalaVersion := "2.11.4",
  crossScalaVersions := Seq("2.10.4", "2.11.4"),
  libraryDependencies ++= Seq(
    "com.twitter" %% "finagle-mux" % "6.24.0",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test"
  ),
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  resolvers += "Twitter's Repository" at "http://maven.twttr.com/",
  parallelExecution in Test := false
)

lazy val root = project.in(file("."))
  .settings(moduleName := "finagle-serial")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .aggregate(core, pickling, scodec, benchmark)

lazy val core = project.in(file("serial-core"))
  .settings(moduleName := "finagle-serial-core")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)

lazy val pickling = project.in(file("serial-pickling"))
  .settings(moduleName := "finagle-serial-pickling")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(libraryDependencies += "org.scala-lang" %% "scala-pickling" % "0.9.1")
  .dependsOn(core)

lazy val scodec = project.in(file("serial-scodec"))
  .settings(moduleName := "finagle-serial-scodec")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(libraryDependencies += "org.typelevel" %% "scodec-core" % "1.6.0")
  .dependsOn(core)

lazy val benchmark = project.in(file("serial-benchmark"))
  .settings(moduleName := "finagle-serial-bencrhmark")
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(jmhSettings: _*)
  .dependsOn(core, pickling, scodec)

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
