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

import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import support.ConnectorIntegrationTest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext.Implicits.global

class IncomeTaxUserDataConnectorSpec extends ConnectorIntegrationTest {

  private val taxYear = 2022
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> aUser.mtditid, "X-Session-ID" -> aUser.sessionId)

  private lazy val underTest = new IncomeTaxUserDataConnector(httpClient, appConfig)

  "IncomeTaxUserDataConnector" should {
    "Return a success result" when {
      "submission returns a 204" in {
        stubGetWithHeadersCheck(s"/income-tax/nino/${aUser.nino}/sources/session\\?taxYear=$taxYear", NO_CONTENT, responseBody = "{}")

        await(underTest.getUserData(aUser.nino, taxYear)) shouldBe Right(IncomeTaxUserData())
      }

      "submission returns a 200" in {
        val expectedResponse =
          """{
            |  "cis": {
            |    "customerCISDeductions": {
            |      "totalDeductionAmount": 100.00,
            |      "totalCostOfMaterials": 50.00,
            |      "totalGrossAmountPaid": 450.00,
            |      "cisDeductions": [
            |        {
            |          "fromDate": "2020-04-06",
            |          "toDate": "2021-04-05",
            |          "contractorName": "ABC Steelworks",
            |          "employerRef": "123/AB123456",
            |          "totalDeductionAmount": 100.00,
            |          "totalCostOfMaterials": 50.00,
            |          "totalGrossAmountPaid": 450.00,
            |          "periodData": [
            |            {
            |              "deductionFromDate": "2020-04-06",
            |              "deductionToDate": "2020-05-05",
            |              "deductionAmount": 100,
            |              "costOfMaterials": 50,
            |              "grossAmountPaid": 450,
            |              "submissionDate": "2020-05-11T16:38:57.489Z",
            |              "submissionId": "submissionId",
            |              "source": "customer"
            |            }
            |          ]
            |        }
            |      ]
            |    },
            |    "contractorCISDeductions": {
            |      "totalDeductionAmount": 100,
            |      "totalCostOfMaterials": 50,
            |      "totalGrossAmountPaid": 450,
            |      "cisDeductions": [
            |        {
            |          "fromDate": "2020-04-06",
            |          "toDate": "2021-04-05",
            |          "contractorName": "ABC Steelworks",
            |          "employerRef": "123/AB123456",
            |          "totalDeductionAmount": 100,
            |          "totalCostOfMaterials": 50,
            |          "totalGrossAmountPaid": 450,
            |          "periodData": [
            |            {
            |              "deductionFromDate": "2020-04-06",
            |              "deductionToDate": "2020-05-05",
            |              "deductionAmount": 100,
            |              "costOfMaterials": 50,
            |              "grossAmountPaid": 450,
            |              "submissionDate": "2020-05-11T16:38:57.489Z",
            |              "submissionId": "submissionId",
            |              "source": "customer"
            |            }
            |          ]
            |        }
            |      ]
            |    }
            |  }
            |}""".stripMargin

        stubGetWithHeadersCheck(s"/income-tax/nino/${aUser.nino}/sources/session\\?taxYear=$taxYear", OK,
          expectedResponse, "X-Session-ID" -> aUser.sessionId, "mtditid" -> aUser.mtditid)

        await(underTest.getUserData(aUser.nino, taxYear)) shouldBe Right(anIncomeTaxUserData)
      }
    }

    "Return an error result" when {
      "the header carrier not as expected" in {
        val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> aUser.mtditid)

        stubGetWithHeadersCheck(s"/income-tax/nino/${aUser.nino}/sources/session\\?taxYear=$taxYear", OK,
          Json.toJson(anIncomeTaxUserData).toString(), "X-Session-ID" -> aUser.sessionId, "mtditid" -> aUser.mtditid)

        await(underTest.getUserData(aUser.nino, taxYear)(hc)) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 200 but invalid json" in {
        stubGetWithHeadersCheck(s"/income-tax/nino/${aUser.nino}/sources/session\\?taxYear=$taxYear", OK,
          Json.toJson("""{"invalid": true}""").toString())

        await(underTest.getUserData(aUser.nino, taxYear)) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {
        stubGetWithHeadersCheck(s"/income-tax/nino/${aUser.nino}/sources/session\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR,
          """{"code": "FAILED", "reason": "failed"}""")

        await(underTest.getUserData(aUser.nino, taxYear)) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {
        stubGetWithHeadersCheck(s"/income-tax/nino/${aUser.nino}/sources/session\\?taxYear=$taxYear", SERVICE_UNAVAILABLE,
          """{"code": "FAILED", "reason": "failed"}""")

        await(underTest.getUserData(aUser.nino, taxYear)) shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {
        stubGetWithHeadersCheck(s"/income-tax/nino/${aUser.nino}/sources/session\\?taxYear=$taxYear", BAD_REQUEST,
          """{"code": "FAILED", "reason": "failed"}""")

        await(underTest.getUserData(aUser.nino, taxYear)) shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }
}