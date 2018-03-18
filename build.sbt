import sbtrelease.ReleaseStateTransformations._
// *****************************************************************************
// Projects
// *****************************************************************************

lazy val core = (project in file("core"))
  .configs(IntegrationTest)
  .settings(commonSettings:  _*)
  .settings(publishSettings: _*)
  .settings(licenses += ("MIT", url("http://opensource.org/licenses/MIT")))
  .settings(Defaults.itSettings: _*)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    moduleName := "arweave4s-core",
    name := "Arweave4s Core",
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion),
    buildInfoPackage := "co.upvest.arweave4s",
    libraryDependencies ++= Seq(
      // compile time dependencies
      library.circeCore         % Compile,
      library.circeParser       % Compile,
      library.sttpCore          % Compile,
      library.spongyCastleCore  % Compile,
      // test dependencies
      library.scalaCheck        % "it,test",
      library.scalaTest         % "it,test"
    ).map(dependencies =>
      library.exclusions.foldRight(dependencies) { (rule, module) =>
        module.excludeAll(rule)
    })
  )

// *****************************************************************************
// Dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val circe        = "0.9.1"
      val scalaCheck   = "1.13.5"
      val scalaTest    = "3.0.5"
      val sttp         = "1.1.9"
      val spongyCastle = "1.58.0.0"
    }
    val circeCore           = "io.circe"                   %% "circe-core"                  % Version.circe
    val circeParser         = "io.circe"                   %% "circe-parser"                % Version.circe
    val sttpCore            = "com.softwaremill.sttp"      %% "core"                        % Version.sttp
    val spongyCastleCore    = "com.madgag.spongycastle"    %  "core"                        % Version.spongyCastle
    val scalaCheck          = "org.scalacheck"             %% "scalacheck"                  % Version.scalaCheck
    val scalaTest           = "org.scalatest"              %% "scalatest"                   % Version.scalaTest

    // All exclusions that should be applied to every module.fo
    val exclusions = Seq(
      //ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12")
    )
  }

// *****************************************************************************
// Settings
// *****************************************************************************

organization in ThisBuild := "co.upvest"

lazy val tagName = Def.setting{
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",
  organization := "co.upvest",
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-target:jvm-1.8",
    "-encoding",
    "UTF-8",
    "-Xfatal-warnings",
    "-Ywarn-unused-import",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-unused-import",
    "-Ypartial-unification",
    "-Xmacro-settings:materialize-derivations"
  ),
  javacOptions ++= Seq(
    "-source",
    "1.8",
    "-target",
    "1.8"
  ),
  cancelable in Global := true,
  fork in Global := true
)


lazy val credentialSettings = Seq(
  credentials ++= (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
)

lazy val sharedPublishSettings = Seq(
  releaseTagName := tagName.value,
  useGpg := true,
  pgpReadOnly := false,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  pgpPassphrase := Option(System.getenv().get("PGP_PASS")).map(_.toCharArray),
  releaseVcsSign := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("Snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("Releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    releaseStepCommand("validate"),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges)
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
) ++ credentialSettings ++ sharedPublishSettings ++ sharedPublishSettings



