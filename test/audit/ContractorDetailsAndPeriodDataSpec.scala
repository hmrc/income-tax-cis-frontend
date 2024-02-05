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
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.audit.ContractorDetailsAndPeriodDataBuilder.aContractorDetailsAndPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

import java.time.Month

class ContractorDetailsAndPeriodDataSpec extends UnitTest {

  ".mapFrom(deductions, deductionMonth)" should {
    "return a ContractorDetailsAndPeriodData case class when the deductionMonth is equal to deductionPeriod" in {
      ContractorDetailsAndPeriodData.mapFrom(aCisDeductions, aPeriodData.deductionPeriod) shouldBe Some(aContractorDetailsAndPeriodData)
    }

    "return None" when {
      "deductionMonth is not equal to deductionPeriod" in {
        ContractorDetailsAndPeriodData.mapFrom(aCisDeductions, Month.JUNE) shouldBe None
      }
      "periodData cannot be found" in {
        ContractorDetailsAndPeriodData.mapFrom(aCisDeductions.copy(periodData = Seq.empty), aPeriodData.deductionPeriod) shouldBe None
      }
    }
  }

  ".mapFrom(employerRef, cisCYAModel)" should {
    "return a ContractorDetailsAndPeriodData case class when periodData exists" in {
      ContractorDetailsAndPeriodData.mapFrom(aCisUserData.employerRef, aCisCYAModel) shouldBe Some(aContractorDetailsAndPeriodData.copy(
        labour = Some(500.00),
        materialsCost = Some(250.00)
      ))
    }
    "return None when periodData is None" in {
      ContractorDetailsAndPeriodData.mapFrom(aCisUserData.employerRef, aCisCYAModel.copy(periodData = None)) shouldBe None
    }
  }

  "writes" should {
    "produce valid json when passed a ContractorDetailsAndPeriodData model" in {
      val json = Json.parse(
        """
          |{
          |    "name": "ABC Steelworks",
          |    "ern": "123/AB123456",
          |    "month": "MAY",
          |    "labour": 450,
          |    "cisDeduction": 100,
          |    "paidForMaterials": true,
          |    "materialsCost": 50
          |  }
          |""".stripMargin
      )

      Json.toJson(ContractorDetailsAndPeriodData.mapFrom(aCisDeductions, aPeriodData.deductionPeriod)) shouldBe json
    }
  }
}
