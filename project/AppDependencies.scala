/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.*

object AppDependencies {

  private val bootstrapPlay30Version = "10.7.0"
  private val mongoPlay30Version = "2.12.0"

  val jacksonAndPlayExclusions: Seq[InclusionRule] = Seq(
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
    ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
    ExclusionRule(organization = "com.fasterxml.jackson.module"),
    ExclusionRule(organization = "com.fasterxml.jackson.core:jackson-annotations"),
    ExclusionRule(organization = "com.typesafe.play")
  )

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30"   % bootstrapPlay30Version,
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30"   % "12.32.0",
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"           % mongoPlay30Version,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"         % "2.19.0",
    "com.beachape"                  %% "enumeratum"                   % "1.9.0",
    "com.beachape"                  %% "enumeratum-play-json"         % "1.9.0" excludeAll (jacksonAndPlayExclusions: _*)
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"     % bootstrapPlay30Version    % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"    % mongoPlay30Version        % Test,
    "org.scalatest"          %% "scalatest"                  % "3.2.19"                  % Test,
    "org.jsoup"              %  "jsoup"                      % "1.22.1"                  % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"         % "7.0.2"                   % Test,
    "org.wiremock"           %  "wiremock-standalone"        % "3.13.1"                  % Test,
    "org.mockito"            %  "mockito-core"               % "5.12.0"                  % Test,
    "org.scalatestplus"      %% "mockito-5-12"               % "3.2.19.0"                % Test
  )
}
