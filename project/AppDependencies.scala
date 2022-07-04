import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  private val bootstrapPlay28Version = "5.24.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-28"   % bootstrapPlay28Version,
    "uk.gov.hmrc"                   %% "play-frontend-hmrc"           % "3.8.0-play-28",
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-28"           % "0.66.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"         % "2.12.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"               %% "bootstrap-test-play-28"     % bootstrapPlay28Version    % Test,
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-test-play-28"    % "0.66.0"                  % Test,
    "org.scalatest"             %% "scalatest"                  % "3.2.9"                   % Test,
    "org.jsoup"                 %  "jsoup"                      % "1.13.1"                  % Test,
    "com.typesafe.play"         %% "play-test"                  % current                   % Test,
    "org.scalatestplus.play"    %% "scalatestplus-play"         % "5.1.0"                   % "test, it",
    "com.github.tomakehurst"    %  "wiremock-jre8"              % "2.28.0"                  % "test, it",
    "org.scalamock"             %% "scalamock"                  % "5.1.0"                   % Test,
    "com.vladsch.flexmark"      %  "flexmark-all"               % "0.36.8"                  % "test, it"
  )
}
