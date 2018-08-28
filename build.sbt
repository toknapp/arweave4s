// *****************************************************************************
// Projects
// *****************************************************************************

lazy val IT = config("it") extend Test

lazy val types = (project in file("types"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(licenses += ("MIT", url("http://opensource.org/licenses/MIT")))
  .settings(
    moduleName := "arweave4s-types",
    name := "Arweave4s Types",
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion),
    buildInfoPackage := "co.upvest.arweave4s",
    libraryDependencies ++= Seq(
      library.circeCore % Compile,
      library.circeParser % Compile,
      library.sttpCore % Compile,
      library.spongyCastleCore % Compile,
    )
  )

lazy val test = (project in file("test"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(licenses += ("MIT", url("http://opensource.org/licenses/MIT")))
  .dependsOn(types)
  .settings(
    moduleName := "arweave4s-test",
    name := "Arweave4s Tests",
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion),
    buildInfoPackage := "co.upvest.arweave4s",
    libraryDependencies ++= Seq(
      library.scalaTest % Test,
      library.scalaCheck % Compile,
    )
  )

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(licenses += ("MIT", url("http://opensource.org/licenses/MIT")))
  .configs(IntegrationTest)
  .settings(inConfig(IT)(Defaults.testSettings))
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(types, test % Test)
  .settings(
    moduleName := "arweave4s-core",
    name := "Arweave4s Core",
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion),
    buildInfoPackage := "co.upvest.arweave4s",
    libraryDependencies ++= Seq(
      // compiler plugins
      compilerPlugin(library.kindProjector),
      // compile time dependencies
      library.circeCore         % Compile,
      library.circeParser       % Compile,
      library.sttpCore          % Compile,
      library.sttpCirce         % Compile,
      // test dependencies
      library.scalaCheck        % "it",
      library.scalaTest         % "it",
      library.sttpAsyncBackend  % "it",
      library.logback           % "it"
    ).map(dependencies =>
      library.exclusions.foldRight(dependencies) { (rule, module) =>
        module.excludeAll(rule)
    })
  )

lazy val root = (project in file("."))
  .settings(publishSettings)
  .aggregate(core, test, types)

// *****************************************************************************
// Dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val circe         = "0.10.0-M1"
      val scalaCheck    = "1.14.0"
      val scalaTest     = "3.0.5"
      val sttp          = "1.2.2"
      val spongyCastle  = "1.58.0.0"
      val kindProjector = "0.9.7"
      val logback       = "1.2.3"
    }
    val circeCore           = "io.circe"                   %% "circe-core"                       % Version.circe
    val circeParser         = "io.circe"                   %% "circe-parser"                     % Version.circe
    val sttpCore            = "com.softwaremill.sttp"      %% "core"                             % Version.sttp
    val sttpCirce           = "com.softwaremill.sttp"      %% "circe"                            % Version.sttp
    val sttpAsyncBackend    = "com.softwaremill.sttp"      %% "async-http-client-backend-future" % Version.sttp
    val spongyCastleCore    = "com.madgag.spongycastle"    %  "core"                             % Version.spongyCastle
    val scalaCheck          = "org.scalacheck"             %% "scalacheck"                       % Version.scalaCheck
    val scalaTest           = "org.scalatest"              %% "scalatest"                        % Version.scalaTest
    val kindProjector       = "org.spire-math"             %% "kind-projector"                   % Version.kindProjector
    val logback             = "ch.qos.logback"             %  "logback-classic"                  % Version.logback


    // All exclusions that should be applied to every module
    val exclusions = Seq()
  }

// *****************************************************************************
// Settings
// *****************************************************************************

organization in ThisBuild := "co.upvest"

lazy val tagName = Def.setting{
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

lazy val commonSettings = Seq(
  scalaVersion := "2.12.6",
  organization := "co.upvest",
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-unused-import",
    "-Ypartial-unification",
    "-Xmacro-settings:materialize-derivations",
    "-Xfuture",
    "-Ycache-plugin-class-loader:last-modified",
    "-Ycache-macro-class-loader:last-modified"
  ),
  scalacOptions in (Compile, console) ~= {
    _ filterNot (_ == "-Ywarn-unused-import")
  },
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  cancelable in Global := true,
  fork in Global := true,
)

lazy val credentialSettings = Seq(
  credentials ++= (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
)

import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Version

def checkoutBranch(branch: String): ReleaseStep = { st: State =>
  val Some(vcs) = Project.extract(st).get(releaseVcs)
  val 0 = vcs.cmd("checkout", branch).!
  st
}

lazy val releaseSettings = Seq(
  releaseTagName := tagName.value,
  useGpg := true,
  pgpReadOnly := true,
  usePgpKeyHex("5C90DFE428FC2B33"),
  pgpPassphrase := Some(Array.empty),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseVcsSign := true,
  releaseVersionBump := Version.Bump.Minor,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),
  releaseCommitMessage := "Bumping version",
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("Snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("Releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishConfiguration := publishConfiguration.value.withOverwrite(isSnapshot.value),
  PgpKeys.publishSignedConfiguration := PgpKeys.publishSignedConfiguration.value.withOverwrite(isSnapshot.value),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(isSnapshot.value),
  PgpKeys.publishLocalSignedConfiguration := PgpKeys.publishLocalSignedConfiguration.value.withOverwrite(isSnapshot.value),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,

    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges,

    checkoutBranch("develop"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/toknapp/arweave4s")),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  scmInfo := Some(ScmInfo(url("https://github.com/toknapp/arweave4s"), "scm:git@github.com:toknapp/arweave4s.git")),
  autoAPIMappings := true,
  apiURL := Some(url("https://tech.upvest.co/arweave4s/api/")),
  pomExtra := (
    <developers>
      <developer>
        <id>allquantor</id>
        <name>Ivan Morozov</name>
        <url>https://github.com/allquantor/</url>
      </developer>
      <developer>
        <id>rootmos</id>
        <name>Gustav Behm</name>
        <url>https://github.com/rootmos/</url>
      </developer>
    </developers>
    )
) ++ credentialSettings ++ releaseSettings
