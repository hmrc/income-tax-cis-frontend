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

package models

import support.UnitTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData

import java.time.Month

class CisDeductionsSpec extends UnitTest {

  ".submissionId" should {
    "return submission id if any PeriodData contains one" in {
      val underTest = aCisDeductions.copy(periodData = Seq(
        aPeriodData.copy(submissionId = None),
        aPeriodData.copy(submissionId = Some("some-submission-id"))
      ))

      underTest.submissionId shouldBe Some("some-submission-id")
    }

    "returns None if none of the PeriodData items contains a reference" in {
      val underTest = aCisDeductions.copy(periodData = Seq(
        aPeriodData.copy(submissionId = None),
        aPeriodData.copy(submissionId = None)
      ))

      underTest.submissionId shouldBe None
    }
  }

  ".periodDataFor" should {
    "return PeriodData for a given month when exists" in {
      val mayPeriodData = aPeriodData.copy(deductionPeriod = Month.MAY)
      val junePeriodData = aPeriodData.copy(deductionPeriod = Month.JUNE)
      val underTest = aCisDeductions.copy(periodData = Seq(mayPeriodData, junePeriodData))

      underTest.periodDataFor(Month.JUNE) shouldBe Some(junePeriodData)
    }

    "return None when no PeriodData exist for a given month" in {
      val mayPeriodData = aPeriodData.copy(deductionPeriod = Month.MAY)
      val junePeriodData = aPeriodData.copy(deductionPeriod = Month.JUNE)
      val underTest = aCisDeductions.copy(periodData = Seq(mayPeriodData, junePeriodData))

      underTest.periodDataFor(Month.JULY) shouldBe None
    }
  }
}
