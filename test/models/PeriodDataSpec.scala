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

package models

import play.api.libs.json.{JsObject, Json}
import support.TaxYearUtils.taxYearEOY
import support.UnitTest
import support.builders.models.PeriodDataBuilder.aPeriodData
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import utils.PagerDutyHelper

import java.time.Month

class PeriodDataSpec extends UnitTest with LogCapturing {

  "PeriodData" should {
    "write to Json correctly when using implicit writes" in {
      val actualResult = Json.toJson(aPeriodData)
      val aGetPeriodDataJsonWrites: JsObject =
        Json.obj(
          "deductionPeriod" -> Month.MAY.toString,
          "deductionAmount" -> Some(100.00),
          "costOfMaterials" -> Some(50.00),
          "grossAmountPaid" -> Some(450.00),
          "submissionDate" -> s"${taxYearEOY - 2}-05-11T16:38:57.489Z",
          "submissionId" -> Some("submissionId"),
          "source" -> "customer"
        )

      actualResult shouldBe aGetPeriodDataJsonWrites
    }

    "read to Json correctly when using implicit read" which {

      "returns the month when a valid month and day is parsed" in {
        val result =
          s"""{
             |   "deductionFromDate": "${taxYearEOY - 2}-04-06",
             |   "deductionToDate": "${taxYearEOY - 2}-05-05",
             |   "submissionDate": "${taxYearEOY - 2}-05-11T16:38:57.489Z",
             |   "source": "customer"
             |}""".stripMargin

        val data: PeriodData = Json.parse(result).as[PeriodData]

        data.deductionPeriod shouldBe Month.MAY
      }

      "returns a month and logs when the month is invalid" in {
        val result =
          s"""{
             |   "deductionFromDate": "${taxYearEOY - 2}-04-06",
             |   "deductionToDate": "${taxYearEOY - 2}-07-05",
             |   "submissionDate": "${taxYearEOY - 2}-05-11T16:38:57.489Z",
             |   "source": "customer"
             |}""".stripMargin

        withCaptureOfLoggingFrom(PagerDutyHelper.logger) {
          logs =>
            val data: PeriodData = Json.parse(result).as[PeriodData]
            data.deductionPeriod shouldBe Month.JULY

            logs.map(_.toString).contains("[ERROR] INVALID_PERIOD_DATES [PeriodData][validatePeriodDatesAndReturnMonth]" +
              s" The retrieved period dates are invalid. fromDate - ${taxYearEOY - 2}-04-06, toDate - ${taxYearEOY - 2}-07-05") shouldBe true
        }
      }

      "returns a month and logs when the day is invalid" in {
        val result =
          s"""{
             |   "deductionFromDate": "${taxYearEOY - 2}-04-06",
             |   "deductionToDate": "${taxYearEOY - 2}-05-02",
             |   "submissionDate": "${taxYearEOY - 2}-05-11T16:38:57.489Z",
             |   "source": "customer"
             |}""".stripMargin

        withCaptureOfLoggingFrom(PagerDutyHelper.logger) {
          logs =>
            val data: PeriodData = Json.parse(result).as[PeriodData]
            data.deductionPeriod shouldBe Month.MAY

            logs.map(_.toString).contains("[ERROR] INVALID_PERIOD_DATES [PeriodData][validatePeriodDatesAndReturnMonth]" +
              s" The retrieved period dates are invalid. fromDate - ${taxYearEOY - 2}-04-06, toDate - ${taxYearEOY - 2}-05-02") shouldBe true
        }
      }
    }

    "throw an exception when the period dates cannot be parsed" in {
      withCaptureOfLoggingFrom(PagerDutyHelper.logger) {
        logs =>
          val caught =
            intercept[Exception] {
              val result =
                s"""{
                   |   "deductionFromDate": "${taxYearEOY - 2}-04-06",
                   |   "deductionToDate": "invalid-to-date",
                   |   "submissionDate": "${taxYearEOY - 2}-05-11T16:38:57.489Z",
                   |   "source": "customer"
                   |}""".stripMargin

              Json.parse(result).as[PeriodData]
            }

          logs.map(_.toString).contains("[ERROR] INVALID_PERIOD_DATES [PeriodData][validatePeriodDatesAndReturnMonth] "
            + "The retrieved period dates are invalid. Text 'invalid-to-date' could not be parsed at index 0") shouldBe true
          assert(caught.getMessage == "The retrieved period dates are invalid.")
      }
    }
  }
}
