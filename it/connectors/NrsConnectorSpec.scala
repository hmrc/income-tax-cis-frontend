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

package connectors

import models.{APIErrorBodyModel, APIErrorModel}
import play.mvc.Http.Status._
import support.ConnectorIntegrationTest
import support.builders.models.UserBuilder.aUser
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class NrsConnectorSpec extends ConnectorIntegrationTest {

  private lazy val underTest: NrsConnector = new NrsConnector(httpClient, appConfig)

  private implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(aUser.sessionId)))

  private val url: String = s"/income-tax-nrs-proxy/${aUser.nino}/itsa-personal-income-submission"

  ".NrsConnector" should {
    "return an Accepted response when successful" in {
      stubPost(url, ACCEPTED, "{}")

      Await.result(underTest.postNrsConnector(aUser.nino, "cis"), Duration.Inf) shouldBe Right(())
    }

    "return an InternalServerError" in {
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Internal Server Error"))

      stubPost(url, INTERNAL_SERVER_ERROR, expectedResult.toJson.toString())

      Await.result(underTest.postNrsConnector(aUser.nino, "cis"), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a NotFound error" in {
      val expectedResult = APIErrorModel(NOT_FOUND, APIErrorBodyModel("NOT_FOUND", "NRS returning not found error"))

      stubPost(url, NOT_FOUND, expectedResult.toJson.toString())

      Await.result(underTest.postNrsConnector(aUser.nino, "cis"), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a ParsingError when an unexpected error has occurred" in {
      val expectedResult = APIErrorModel(CONFLICT, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API"))

      stubPost(url, CONFLICT, expectedResult.toJson.toString())

      Await.result(underTest.postNrsConnector(aUser.nino, "cis"), Duration.Inf) shouldBe Left(expectedResult)
    }
  }
}