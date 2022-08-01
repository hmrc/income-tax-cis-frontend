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

package models.nrs

import play.api.libs.json.Json
import support.UnitTest

import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

class PreviousCisContractorSpec extends UnitTest {
  "writes" should {
    "produce valid json when passed a PreviousCisContractor" in {
      val json = Json.parse(
        """
          | {
          |    "contractorName": "ABC Steelworks",
          |    "ERN": "123/AB123456",
          |    "deductionPeriods": [
          |      {
          |        "month": "MAY",
          |        "labour": 500,
          |        "cisDeduction": 100,
          |        "costOfMaterialsQuestion": true,
          |        "materialsCost": 250
          |      }
          |    ],
          |    "customerDeductionPeriods": [
          |      {
          |        "month": "MAY",
          |        "labour": 500,
          |        "cisDeduction": 100,
          |        "costOfMaterialsQuestion": true,
          |        "materialsCost": 250
          |      }
          |    ]
          |  }
          |""".stripMargin
      )

      val deductionPeriods = Seq(DeductionPeriod(aCisUserData.cis.periodData.get))
      val customerDeductionPeriods = Seq(DeductionPeriod(aCisUserData.cis.periodData.get))
      val previousCisContractor = PreviousCisContractor(contractorName = "ABC Steelworks", ern = "123/AB123456",
        deductionPeriods = deductionPeriods, customerDeductionPeriods = customerDeductionPeriods)

      Json.toJson(previousCisContractor) shouldBe json
    }
  }
}
