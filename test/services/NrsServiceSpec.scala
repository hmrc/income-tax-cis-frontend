/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import connectors.NrsConnector
import connectors.parsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsString, Writes}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Future

class NrsServiceSpec extends UnitTest
  with FutureAwaits with DefaultAwaitTimeout
  with MockFactory {

  private val nino = "AA123456A"
  private val mtditid = "1234567890"

  implicit private val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(aUser.sessionId)))
  implicit private val writesObject: Writes[String] = (o: String) => JsString(o)
  private val connector: NrsConnector = mock[NrsConnector]

  private val underTest: NrsService = new NrsService(connector)

  ".postNrsConnector" when {
    "there is a true client ip and port" should {
      "return the connector response" in {
        val expectedResult: NrsSubmissionResponse = Right()
        val headerCarrierWithTrueClientDetails = headerCarrierWithSession.copy(trueClientIp = Some("127.0.0.1"), trueClientPort = Some("80"))

        (connector.postNrsConnector(_: String, _: String)(_: HeaderCarrier, _: Writes[String]))
          .expects(nino, "cis", headerCarrierWithTrueClientDetails.withExtraHeaders("mtditid" -> mtditid, "clientIP" -> "127.0.0.1", "clientPort" -> "80"), writesObject)
          .returning(Future.successful(expectedResult))

        val result = await(underTest.submit(nino, "cis", mtditid)(headerCarrierWithTrueClientDetails, writesObject))

        result shouldBe expectedResult
      }
    }

    "there isn't a true client ip and port" should {
      "return the connector response" in {
        val expectedResult: NrsSubmissionResponse = Right()

        (connector.postNrsConnector(_: String, _: String)(_: HeaderCarrier, _: Writes[String]))
          .expects(nino, "cis", headerCarrierWithSession.withExtraHeaders("mtditid" -> mtditid), writesObject)
          .returning(Future.successful(expectedResult))

        val result = await(underTest.submit(nino, "cis", mtditid))

        result shouldBe expectedResult
      }
    }
  }
}