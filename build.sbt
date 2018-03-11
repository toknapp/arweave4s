import sbt.Keys.logLevel
// *****************************************************************************
// Projects
// *****************************************************************************

lazy val core = (project in file("core"))
  .configs(IntegrationTest)
  .settings(commonSettings: _*)
  .settings(Defaults.itSettings: _*)
  .settings(licenses += ("MIT", url("http://opensource.org/licenses/MIT")))
  .settings(inConfig(IntegrationTest)(scalafmtSettings): _*)
  .enablePlugins(GitVersioning, GitBranchPrompt, BuildInfoPlugin)
  .settings(
    name := "arweave4s-core",
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion, sbtVersion),
    buildInfoPackage := "co.upvest.arweave4s",
    skip in publish := true,
    libraryDependencies ++= Seq(
      // compile time dependencies
      library.circeCore         % Compile,
      library.circeGeneric      % Compile,
      library.circeParser       % Compile,
      library.pureConfig        % Compile,
      library.sttpCore          % Compile,
      library.spongyCastleCore  % Compile,
      // setup logging
      library.log4jApi          % Compile,
      library.logJulOverLog4j   % Compile,
      library.logSlfOverLog4j   % Compile,
      library.log4jCore         % Compile,
      library.scalaLogging      % Compile,
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
      val log4j        = "2.10.0"
      val pureConfig   = "0.9.0"
      val scalaCheck   = "1.13.5"
      val scalaFmt     = "1.4.0"
      val scalaLogging = "3.7.2"
      val scalaTest    = "3.0.5"
      val scryptoV     = "2.0.5"
      val scodecV      = "1.1.5"
      val sttp         = "1.1.9"
      val spongyCastle = "1.58.0.0"
    }
    val circeCore           = "io.circe"                   %% "circe-core"                  % Version.circe
    val circeGeneric        = "io.circe"                   %% "circe-generic"               % Version.circe
    val circeParser         = "io.circe"                   %% "circe-parser"                % Version.circe
    val sttpCore            = "com.softwaremill.sttp"      %% "core"                        % Version.sttp
    val spongyCastleCore    = "com.madgag.spongycastle"    %  "core"                        % Version.spongyCastle
    val log4jApi            = "org.apache.logging.log4j"   % "log4j-api"                    % Version.log4j
    val log4jCore           = "org.apache.logging.log4j"   % "log4j-core"                   % Version.log4j
    val logSlfOverLog4j     = "org.apache.logging.log4j"   % "log4j-slf4j-impl"             % Version.log4j
    val logJulOverLog4j     = "org.apache.logging.log4j"   % "log4j-jul"                    % Version.log4j
    val pureConfig          = "com.github.pureconfig"      %% "pureconfig"                  % Version.pureConfig
    val scalaCheck          = "org.scalacheck"             %% "scalacheck"                  % Version.scalaCheck
    val scalaLogging        = "com.typesafe.scala-logging" %% "scala-logging"               % Version.scalaLogging
    val scalaTest           = "org.scalatest"              %% "scalatest"                   % Version.scalaTest

    // All exclusions that should be applied to every module.
    val exclusions = Seq(
      ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
      ExclusionRule(organization = "log4j", name = "log4j"),
      ExclusionRule(organization = "ch.qos.logback", name = "logback-classic")
    )
  }

// *****************************************************************************
// Settings
// *****************************************************************************

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
  javaOptions ++= Seq(
    "-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"
  ),
  cancelable in Global := true,
  git.useGitDescribe := true,
  git.formattedShaVersion := Some(scala.util.Properties.envOrElse("CIRCLE_SHA1", "latestLocal"))
    .map(_.take(7)), // use git short head version
  scalafmtOnCompile := true,
  scalafmtVersion := library.Version.scalaFmt,
  fork in Global := true
)



