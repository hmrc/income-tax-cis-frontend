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

package support

import models.session.SessionData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import support.builders.models.UserBuilder.{aUser, anAgentUser}

trait UnitTest extends AnyWordSpec
  with FutureAwaits with DefaultAwaitTimeout
  with Matchers {

  val sessionId: String = aUser.sessionId
  val mtditid: String = aUser.mtditid
  val nino: String = aUser.nino
  val arn: String = anAgentUser.arn.get

  val sessionData: SessionData = SessionData(sessionId, mtditid, nino)
}