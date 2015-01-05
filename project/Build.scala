import sbt._
import sbt.Keys._
import com.typesafe.sbt.pgp.PgpKeys._

object Serial extends Build {

  val baseSettings = Defaults.defaultSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-mux" % "6.24.0"
    ),
    scalacOptions ++= Seq( "-unchecked", "-deprecation", "-feature")
  )

  lazy val buildSettings = Seq(
    organization := "io.github.finagle",
    version := "0.0.1",
    scalaVersion := "2.11.4",
    crossScalaVersions := Seq("2.10.4", "2.11.4")
  )

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

  lazy val allSettings = baseSettings ++ buildSettings ++ publishSettings

  lazy val root = Project(
    id = "finagle-serial",
    base = file("."),
    settings = allSettings
  ) aggregate(core)

  lazy val core = Project(
    id = "serial-core",
    base = file("serial-core"),
    settings = allSettings
  )
}