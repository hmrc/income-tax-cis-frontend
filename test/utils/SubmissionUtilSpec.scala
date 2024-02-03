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

package utils

import models.submission.CISSubmission
import support.UnitTest
import support.builders.models.submission.PeriodDataBuilder.aPeriodData
import utils.SubmissionUtil.validateDataAndCreateSubmission

class SubmissionUtilSpec extends UnitTest {

  "validateDataAndCreateSubmission" should {
    "return None or Some(submission) depending on whether all of the periods to submit are valid" when {
      "periodDataForSubmission is empty" in {
        validateDataAndCreateSubmission(Seq(),None,Some("name"),"12345") shouldBe None
      }
      "periodDataForSubmission has one invalid period" in {
        validateDataAndCreateSubmission(Seq(Some(aPeriodData), None),None,Some("name"),"12345") shouldBe None
      }
      "periodDataForSubmission has duplicate periods" in {
        validateDataAndCreateSubmission(Seq(Some(aPeriodData), Some(aPeriodData)),None,Some("name"),"12345") shouldBe None
      }
      "periodDataForSubmission has more than 12 periods" in {
        validateDataAndCreateSubmission(Seq(
          Some(aPeriodData.copy(deductionToDate = "2021-01-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-02-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-03-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-04-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-05-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-06-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-07-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-08-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-09-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-10-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-11-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-12-05")),
          Some(aPeriodData.copy(deductionToDate = "2022-01-05"))
        ),None,Some("name"),"12345") shouldBe None
      }
      "periodDataForSubmission has all 12 periods" in {
        val data = Seq(
          Some(aPeriodData.copy(deductionToDate = "2021-05-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-06-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-07-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-08-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-09-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-10-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-11-05")),
          Some(aPeriodData.copy(deductionToDate = "2021-12-05")),
          Some(aPeriodData.copy(deductionToDate = "2022-01-05")),
          Some(aPeriodData.copy(deductionToDate = "2022-02-05")),
          Some(aPeriodData.copy(deductionToDate = "2022-03-05")),
          Some(aPeriodData.copy(deductionToDate = "2022-04-05"))
        )

        validateDataAndCreateSubmission(data,None,Some("name"),"12345") shouldBe Some(
          CISSubmission(
            employerRef = Some("12345"),
            contractorName = Some("name"),
            periodData = data.flatten,
            submissionId = None
          )
        )
      }
      "periodDataForSubmission has 1 valid period" in {
        val data = Seq(
          Some(aPeriodData.copy(deductionToDate = "2022-04-05"))
        )

        validateDataAndCreateSubmission(data,None,Some("name"),"12345") shouldBe Some(
          CISSubmission(
            employerRef = Some("12345"),
            contractorName = Some("name"),
            periodData = data.flatten,
            submissionId = None
          )
        )
      }
      "periodDataForSubmission has 1 valid period but no contractor name" in {
        val data = Seq(
          Some(aPeriodData.copy(deductionToDate = "2022-04-05"))
        )

        validateDataAndCreateSubmission(data,None,None,"12345") shouldBe None
      }
      "periodDataForSubmission has 1 valid period when it's an update to existing data" in {
        val data = Seq(
          Some(aPeriodData.copy(deductionToDate = "2022-04-05"))
        )

        validateDataAndCreateSubmission(data,Some("id"),Some("name"),"12345") shouldBe Some(
          CISSubmission(
            employerRef = None,
            contractorName = None,
            periodData = data.flatten,
            submissionId = Some("id")
          )
        )
      }
    }
  }
}
