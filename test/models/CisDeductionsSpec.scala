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

package models

import models.mongo.{CYAPeriodData, CisCYAModel}
import support.UnitTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData

import java.time.Month

class CisDeductionsSpec extends UnitTest {

  ".recalculateFigures" should {
    "recalculate the amounts when the data is updated" in {
      val underTest = aCisDeductions.copy(periodData = Seq(
        aPeriodData.copy(deductionAmount = Some(3445.53),
          costOfMaterials = Some(435.55),
          grossAmountPaid = Some(33.33)
        ),
        aPeriodData.copy(deductionPeriod = Month.DECEMBER,
          deductionAmount = Some(100.00),
          costOfMaterials = Some(100.00),
          grossAmountPaid = Some(100.00)
        )
      ))

      underTest.recalculateFigures shouldBe underTest.copy(
        totalCostOfMaterials = Some(535.55),
        totalDeductionAmount = Some(3545.53),
        totalGrossAmountPaid = Some(133.33)
      )
    }
    "recalculate the amounts when cost of materials is empty" in {
      val underTest = aCisDeductions.copy(periodData = Seq(
        aPeriodData.copy(deductionAmount = Some(3445.53),
          costOfMaterials = None,
          grossAmountPaid = Some(33.33)
        )
      ))

      underTest.recalculateFigures shouldBe underTest.copy(
        totalCostOfMaterials = None,
        totalDeductionAmount = Some(3445.53),
        totalGrossAmountPaid = Some(33.33)
      )
    }
  }

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

  "toCYA" should {
    val anyBoolean = true
    "return a cya model" in {
      aCisDeductions.toCYA(None, Seq.empty, hasCompleted = anyBoolean) shouldBe CisCYAModel(
        Some("ABC Steelworks"),
        None,
        List(CYAPeriodData(Month.MAY, Some(450.0), Some(100.0), Some(true), Some(50.0), contractorSubmitted = false, Some(Month.MAY)))
      )
    }
    "return a cya model when making an update to existing month" in {
      aCisDeductions.toCYA(Some(Month.MAY), Seq(Month.MAY), anyBoolean) shouldBe CisCYAModel(
        Some("ABC Steelworks"),
        Some(CYAPeriodData(Month.MAY, Some(450.0), Some(100.0), Some(true), Some(50.0), contractorSubmitted = true, Some(Month.MAY))),
        List(CYAPeriodData(Month.MAY, Some(450.0), Some(100.0), Some(true), Some(50.0), contractorSubmitted = true, Some(Month.MAY)))
      )
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
