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

package audit

import play.api.libs.json.Json
import support.UnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.audit.DeductionPeriodDataBuilder.aDeductionPeriodData
import support.builders.models.audit.PreviousCisContractorBuilder.aPreviousCisContractor

import java.time.Month

class PreviousCisContractorSpec extends UnitTest {

  ".apply(employerRef, optionalAllCISDeductions)" should {
    "return a PreviousCisContractor case class" which {
      "has multiple deductionPeriods for contractor data" in {
        val periodData1 = aPeriodData.copy(deductionPeriod = Month.JUNE, deductionAmount = Some(200), grossAmountPaid = Some(350))
        val periodData2 = aPeriodData.copy(deductionPeriod = Month.JULY, grossAmountPaid = Some(300), costOfMaterials = None)
        val periodData3 = aPeriodData.copy(deductionPeriod = Month.JUNE, costOfMaterials = None)
        val periodData4 = aPeriodData.copy(deductionPeriod = Month.FEBRUARY)
        val cisDeduction1 = aCisDeductions.copy(employerRef = "ref-1", contractorName = Some("some-name"), periodData = Seq(periodData1, periodData2))
        val cisDeduction2 = aCisDeductions.copy(employerRef = "ref-1", contractorName = Some("some-name"), periodData = Seq(periodData3, periodData4))

        val allCisDeductions = anAllCISDeductions.copy(
          contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions, cisDeduction1))),
          customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeduction2)))
        )

        PreviousCisContractor.apply("ref-1", Some(allCisDeductions)) shouldBe aPreviousCisContractor.copy(
          contractorName = Some("some-name"),
          ern = "ref-1",
          deductionPeriods = Seq(
            aDeductionPeriodData.copy(month = "JUNE", labour = Some(350), cisDeduction = Some(200), paidForMaterials = true, materialsCost = Some(50)),
            aDeductionPeriodData.copy(month = "JULY", labour = Some(300), cisDeduction = Some(100), paidForMaterials = false, materialsCost = None)
          ),
          customerDeductionPeriods = Seq(
            aDeductionPeriodData.copy(month = "JUNE", labour = Some(450), cisDeduction = Some(100), paidForMaterials = false, materialsCost = None),
            aDeductionPeriodData.copy(month = "FEBRUARY", labour = Some(450), cisDeduction = Some(100), paidForMaterials = true, materialsCost = Some(50))
          )
        )
      }

      "has no cisDeductions" in {
        val allCisDeductions = anAllCISDeductions.copy(
          contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq())),
          customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq()))
        )

        PreviousCisContractor.apply(employerRef = "some-ref", optionalAllCISDeductions = Some(allCisDeductions)) shouldBe aPreviousCisContractor.copy(
          contractorName = None,
          ern = "some-ref",
          deductionPeriods = Seq.empty,
          customerDeductionPeriods = Seq.empty
        )
      }

      "optionalAllCISDeductions is None" in {
        PreviousCisContractor.apply(employerRef = "some-ref", optionalAllCISDeductions = None) shouldBe aPreviousCisContractor.copy(
          contractorName = None,
          ern = "some-ref",
          deductionPeriods = Seq.empty,
          customerDeductionPeriods = Seq.empty
        )
      }
    }
  }

  "writes" should {
    "return a valid json when passed a PreviousCisContractor" in {
      val json = Json.parse(
        """
          |{
          |  "contractorName": "ABC Steelworks",
          |  "ern": "123/4567890",
          |  "deductionPeriods": [
          |    {
          |      "month": "APRIL",
          |      "labour": 280,
          |      "cisDeduction": 120,
          |      "paidForMaterials": true,
          |      "materialsCost": 30
          |    }
          |  ],
          |  "customerDeductionPeriods": [
          |    {
          |      "month": "APRIL",
          |      "labour": 300,
          |      "cisDeduction": 200,
          |      "paidForMaterials": true,
          |      "materialsCost": 100
          |    },
          |    {
          |      "month": "JANUARY",
          |      "labour": 100,
          |      "cisDeduction": 50,
          |      "paidForMaterials": false
          |    }
          |  ]
          |}
          |""".stripMargin)

      val model = PreviousCisContractor(
        contractorName = Some("ABC Steelworks"),
        ern = "123/4567890",
        deductionPeriods = Seq(aDeductionPeriodData.copy(
          month = Month.APRIL.toString, labour = Some(280), cisDeduction = Some(120), paidForMaterials = true, materialsCost = Some(30)
        )),
        customerDeductionPeriods = Seq(
          aDeductionPeriodData.copy(month = Month.APRIL.toString, labour = Some(300), cisDeduction = Some(200), paidForMaterials = true, materialsCost = Some(100)),
          aDeductionPeriodData.copy(month = Month.JANUARY.toString, labour = Some(100), cisDeduction = Some(50), paidForMaterials = false, materialsCost = None)
        )
      )

      Json.toJson(model) shouldBe json
    }
  }
}
