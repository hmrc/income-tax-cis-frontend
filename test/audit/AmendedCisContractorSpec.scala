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

package audit

import play.api.libs.json.Json
import support.UnitTest
import support.builders.models.audit.DeductionPeriodDataBuilder.aDeductionPeriodData
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel

import java.time.Month

class AmendedCisContractorSpec extends UnitTest {

  "writes" should {
    "produce a valid json when passed an AmendedCisContractor case class with multiple deductionPeriods" in {
      val json = Json.parse(
        """
          |{
          |  "contractorName": "ABC Steelworks",
          |  "ern": "123/AB456789",
          |  "deductionPeriods": [
          |    {
          |      "month": "MAY",
          |      "labour": 500,
          |      "cisDeduction": 250,
          |      "paidForMaterials": false
          |    },
          |    {
          |      "month": "FEBRUARY",
          |      "labour": 280,
          |      "cisDeduction": 45,
          |      "paidForMaterials": true,
          |      "materialsCost": 70
          |    },
          |    {
          |      "month": "APRIL",
          |      "labour": 150,
          |      "cisDeduction": 125,
          |      "paidForMaterials": true,
          |      "materialsCost": 30
          |    }
          |  ],
          |  "customerDeductionPeriods": {
          |    "month": "MAY",
          |    "labour": 500,
          |    "cisDeduction": 200,
          |    "paidForMaterials": true,
          |    "materialsCost": 300
          |  }
          |}
          |""".stripMargin)

      val model = AmendedCisContractor(
        contractorName = Some("ABC Steelworks"),
        ern = "123/AB456789",
        deductionPeriods = Seq(
          aDeductionPeriodData.copy(month = Month.MAY.toString, labour = Some(500), cisDeduction = Some(250), paidForMaterials = false, materialsCost = None),
          aDeductionPeriodData.copy(month = Month.FEBRUARY.toString, labour = Some(280), cisDeduction = Some(45), paidForMaterials = true, materialsCost = Some(70)),
          aDeductionPeriodData.copy(month = Month.APRIL.toString, labour = Some(150), cisDeduction = Some(125), paidForMaterials = true, materialsCost = Some(30))
        ),
        customerDeductionPeriods = Some(aDeductionPeriodData.copy(
          month = Month.MAY.toString, labour = Some(500), cisDeduction = Some(200), paidForMaterials = true, materialsCost = Some(300)
        ))
      )

      Json.toJson(model) shouldBe json
    }
  }

  ".apply(employerRef, cisCYAModel)" should {
    "return an AmendedCisContractor from a CisCYAModel" which {
      "has multiple deductionPeriods for priorPeriodData (contractor data)" in {
        val periodData1 = aCYAPeriodData.copy(deductionPeriod = Month.JUNE, deductionAmount = Some(200), grossAmountPaid = Some(350))
        val periodData2 = aCYAPeriodData.copy(deductionPeriod = Month.JULY, grossAmountPaid = Some(300), costOfMaterialsQuestion = Some(false), costOfMaterials = None)
        val periodData3 = aCYAPeriodData.copy(deductionPeriod = Month.JUNE, costOfMaterialsQuestion = Some(false), costOfMaterials = None)

        val cyaModel = aCisCYAModel.copy(contractorName = Some("some-contractor-name"), periodData = Some(periodData3), priorPeriodData = Seq(periodData1, periodData2))

        AmendedCisContractor.apply("ref-1", cyaModel) shouldBe AmendedCisContractor(
          contractorName = Some("some-contractor-name"),
          ern = "ref-1",
          deductionPeriods = Seq(
            aDeductionPeriodData.copy(month = "JUNE", labour = Some(350), cisDeduction = Some(200), paidForMaterials = true, materialsCost = Some(250)),
            aDeductionPeriodData.copy(month = "JULY", labour = Some(300), cisDeduction = Some(100), paidForMaterials = false, materialsCost = None)
          ),
          customerDeductionPeriods = Some(
            aDeductionPeriodData.copy(month = "JUNE", labour = Some(500), cisDeduction = Some(100), paidForMaterials = false, materialsCost = None)
          )
        )
      }

      "has periodData in session but no priorPeriodData (contractor data)" in {
        val cyaModel = aCisCYAModel.copy(contractorName = Some("some-contractor-name"), periodData = Some(aCYAPeriodData), priorPeriodData = Seq())

        AmendedCisContractor.apply("ref-2", cyaModel) shouldBe AmendedCisContractor(
          contractorName = Some("some-contractor-name"),
          ern = "ref-2",
          deductionPeriods = Seq.empty,
          customerDeductionPeriods = Some(aDeductionPeriodData.copy(cisDeduction = Some(100), paidForMaterials = true, materialsCost = Some(250)))
        )
      }
    }
  }
}
