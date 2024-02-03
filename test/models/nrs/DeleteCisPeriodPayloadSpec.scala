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

import play.api.libs.json.Json
import support.UnitTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData

import java.time.Month.NOVEMBER

class DeleteCisPeriodPayloadSpec extends UnitTest {
  "writes" should {
    "produce valid json when passed a DeleteCisContractorPayload" in {
      val json = Json.parse(
        """
          |{
          |  "deletedCisPeriod": {
          |    "contractorDetails": {
          |      "name": "ABC Steelworks",
          |      "ERN": "123/AB123456"
          |    },
          |    "month": "NOVEMBER",
          |    "labour": 450,
          |    "cisDeduction": 100,
          |    "paidForMaterials": true,
          |    "materialsCost": 50
          |  }
          |}
          |""".stripMargin)

      val periodData = aPeriodData.copy(deductionPeriod = NOVEMBER)
      val deleteCisPeriodPayload = DeleteCisPeriodPayload(ContractorDetails(aCisDeductions.contractorName, aCisDeductions.employerRef), periodData)

      Json.toJson(deleteCisPeriodPayload) shouldBe json
    }
  }
}
