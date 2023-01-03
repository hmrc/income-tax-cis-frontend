/*
 * Copyright 2023 HM Revenue & Customs
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

import models.submission.CISSubmission
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.libs.json.Json
import play.mvc.Http.Status._
import support.builders.models.submission.CISSubmissionBuilder.aCISSubmission
import support.{ConnectorIntegrationTest, TaxYearProvider}
import support.builders.models.UserBuilder.aUser
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class CISConnectorSpec extends ConnectorIntegrationTest with TaxYearProvider {

  private lazy val underTest: CISConnector = new CISConnector(httpClient, appConfig)

  private implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(aUser.sessionId)))

  private val url: String = s"/income-tax-cis/income-tax/nino/${aUser.nino}/sources\\?taxYear=$taxYearEOY"
  private val deleteUrl: String = s"/income-tax-cis/income-tax/nino/${aUser.nino}/sources/submissionId\\?taxYear=$taxYearEOY"

  val anUpdateCISSubmission: CISSubmission = aCISSubmission.copy(
    employerRef = None,
    contractorName = None,
    submissionId = Some("submissionId")
  )

  ".submit" should {
    "return an OK response when successful" in {
      stubPost(url, OK, Json.toJson(aCISSubmission).toString())

      Await.result(underTest.submit(aUser.nino, taxYearEOY, aCISSubmission), Duration.Inf) shouldBe Right(())
    }

    "Return an error result" when {
      Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>
        s"CIS BE returns a $status" in {
          stubPost(url, status, Json.toJson(aCISSubmission).toString())

          Await.result(underTest.submit(aUser.nino, taxYearEOY, aCISSubmission), Duration.Inf) shouldBe
            Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
        }
      }

      s"CIS BE returns an unexpected result" in {
        stubPost(url, BAD_GATEWAY, Json.toJson(aCISSubmission).toString())

        Await.result(underTest.submit(aUser.nino, taxYearEOY, aCISSubmission), Duration.Inf) shouldBe
          Left(APIErrorModel(BAD_GATEWAY, APIErrorBodyModel.parsingError))
      }
    }
  }

  ".delete" should {
    "return an NO CONTENT response when successful" in {
      stubDelete(deleteUrl, NO_CONTENT)

      Await.result(underTest.delete(aUser.nino, taxYearEOY, anUpdateCISSubmission.submissionId.get), Duration.Inf) shouldBe Right(())
    }

    "Return an error result" when {
      Seq(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>
        s"CIS BE returns a $status" in {
          stubDelete(deleteUrl, status)

          Await.result(underTest.delete(aUser.nino, taxYearEOY, anUpdateCISSubmission.submissionId.get), Duration.Inf) shouldBe
            Left(APIErrorModel(status, APIErrorBodyModel.parsingError))
        }
      }

      s"CIS BE returns an unexpected result" in {
        stubDelete(deleteUrl, BAD_GATEWAY)

        Await.result(underTest.delete(aUser.nino, taxYearEOY, anUpdateCISSubmission.submissionId.get), Duration.Inf) shouldBe
          Left(APIErrorModel(BAD_GATEWAY, APIErrorBodyModel.parsingError))
      }
    }
  }
}