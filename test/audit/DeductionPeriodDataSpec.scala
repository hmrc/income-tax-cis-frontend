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

package audit

import play.api.libs.json.Json
import support.UnitTest
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.audit.DeductionPeriodDataBuilder.aDeductionPeriodData
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData

import java.time.Month

class DeductionPeriodDataSpec extends UnitTest {

  "writes" should {
    "produce a valid json when passed a DeductionPeriodData" in {
      val json = Json.parse(
        """
          |{
          |  "month": "MAY",
          |  "labour": 300,
          |  "cisDeduction": 200,
          |  "paidForMaterials": true,
          |  "materialsCost": 100
          |}
          |""".stripMargin
      )

      val model = DeductionPeriodData(month = Month.MAY.toString, labour = Some(300), cisDeduction = Some(200),
        paidForMaterials = true, materialsCost = Some(100))

      Json.toJson(model) shouldBe json
    }
  }

  ".apply(cyaPeriodData)" should {
    "return a DeductionPeriodData from CYAPeriodData" which {
      "has no costOfMaterials amount" in {
        val cyaPeriodData = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(false), costOfMaterials = None)

        DeductionPeriodData.apply(cyaPeriodData) shouldBe aDeductionPeriodData.copy(
          month = "MAY",
          labour = Some(500),
          cisDeduction = Some(100),
          paidForMaterials = false,
          materialsCost = None
        )
      }

      "has costOfMaterials defined" in {
        DeductionPeriodData.apply(aCYAPeriodData) shouldBe aDeductionPeriodData.copy(
          month = "MAY",
          labour = Some(500),
          cisDeduction = Some(100),
          paidForMaterials = true,
          materialsCost = Some(250)
        )
      }
    }
  }

  ".apply(periodData)" should {
    "return a DeductionPeriodData from PeriodData" which {
      "has no costOfMaterials amount" in {
        val periodData = aPeriodData.copy(costOfMaterials = None)

        DeductionPeriodData.apply(periodData) shouldBe aDeductionPeriodData.copy(
          month = "MAY",
          labour = Some(450),
          cisDeduction = Some(100),
          paidForMaterials = false,
          materialsCost = None
        )
      }

      "has costOfMaterials defined" in {
        DeductionPeriodData.apply(aPeriodData) shouldBe aDeductionPeriodData.copy(
          month = "MAY",
          labour = Some(450),
          cisDeduction = Some(100),
          paidForMaterials = true,
          materialsCost = Some(50)
        )
      }
    }
  }
}
