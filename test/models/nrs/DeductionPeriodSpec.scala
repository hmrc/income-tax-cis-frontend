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

package models.nrs

import support.UnitTest
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

class DeductionPeriodSpec extends UnitTest {

  "apply" should {
    "return a DeductionPeriod using a CYAPeriodData" in {
      val period = aCisUserData.cis.periodData.get
      val deductionPeriod = DeductionPeriod(period)

      deductionPeriod shouldBe
        DeductionPeriod(
          period.deductionPeriod.toString,
          period.grossAmountPaid,
          period.deductionAmount,
          period.costOfMaterialsQuestion.get,
          period.costOfMaterials)
    }

    "return a DeductionPeriod using a PeriodData" in {
      val deductionPeriod = DeductionPeriod(aPeriodData)

      deductionPeriod shouldBe
        DeductionPeriod(
          aPeriodData.deductionPeriod.toString,
          aPeriodData.grossAmountPaid,
          aPeriodData.deductionAmount,
          aPeriodData.costOfMaterials.isDefined,
          aPeriodData.costOfMaterials
        )
    }
  }
}
