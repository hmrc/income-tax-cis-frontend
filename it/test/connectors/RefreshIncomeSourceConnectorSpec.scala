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

package connectors

import models.{APIErrorBodyModel, APIErrorModel, RefreshIncomeSourceRequest}
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import play.mvc.Http.Status._
import support.builders.models.UserBuilder.aUser
import support.{ConnectorIntegrationTest, TaxYearProvider}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class RefreshIncomeSourceConnectorSpec extends ConnectorIntegrationTest with TaxYearProvider {

  private lazy val underTest: RefreshIncomeSourceConnector = new RefreshIncomeSourceConnector(httpClient, appConfig)

  private implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(aUser.sessionId)))

  private val url: String = s"/income-tax/nino/${aUser.nino}/sources/session\\?taxYear=$taxYearEOY"

  ".RefreshIncomeSourceConnector" should {
    "return a Right() response when successful" in {
      stubPut(url, NO_CONTENT, Json.toJson(RefreshIncomeSourceRequest("cis")).toString())

      Await.result(underTest.put(taxYearEOY, aUser.nino), Duration.Inf) shouldBe Right(())
    }
    "return a Right() response when a 404 response" in {
      stubPut(url, NOT_FOUND, Json.toJson(RefreshIncomeSourceRequest("cis")).toString())

      Await.result(underTest.put(taxYearEOY, aUser.nino), Duration.Inf) shouldBe Right(())
    }

    "Return an error result" when {
      Seq(BAD_REQUEST, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>
        s"Submission BE returns a $status" in {
          stubPut(url, status, Json.toJson(RefreshIncomeSourceRequest("cis")).toString())

          Await.result(underTest.put(taxYearEOY, aUser.nino), Duration.Inf) shouldBe
            Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
        }
      }

      s"Submission BE returns an unexpected result" in {
        stubPut(url, BAD_GATEWAY, Json.toJson(RefreshIncomeSourceRequest("cis")).toString())

        Await.result(underTest.put(taxYearEOY, aUser.nino), Duration.Inf) shouldBe
          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }
    }
  }
}