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
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

class AmendCisContractorPayloadSpec extends UnitTest {

  "apply" should {
    "return an AmendCisContractorPayloadSpec" in {
      val amendCisContractorPayload = AmendCisContractorPayload(employerRef = aCisUserData.employerRef, cisUserData = aCisUserData, incomeTaxUserData = anIncomeTaxUserData)

      val previousCustomerDeductionPeriods = Seq(DeductionPeriod("MAY", Some(450),Some(100), true, Some(50)))
      val previousCisContractor = amendCisContractorPayload.previousContractor
      previousCisContractor.ern shouldBe aCisUserData.employerRef
      previousCisContractor.contractorName shouldBe "ABC Steelworks"
      previousCisContractor.customerDeductionPeriods shouldBe previousCustomerDeductionPeriods
      previousCisContractor.deductionPeriods shouldBe previousCustomerDeductionPeriods

      val newDeductionPeriods = Seq(DeductionPeriod("NOVEMBER", Some(500),Some(100), true, Some(250)))
      val newCustomerDeductionPeriod = Some(DeductionPeriod("MAY", Some(500),Some(100), true, Some(250)))
      val newCisContractor = amendCisContractorPayload.newContractor
      newCisContractor.ern shouldBe aCisUserData.employerRef
      newCisContractor.contractorName shouldBe "ABC Steelworks"
      newCisContractor.deductionPeriods shouldBe newDeductionPeriods
      newCisContractor.customerDeductionPeriods shouldBe newCustomerDeductionPeriod
    }
  }

  "writes" should {
    "produce valid json when passed a AmendCisContractorPayload" in {
      val json = Json.parse(
        """
          |{
          |  "previousContractor": {
          |    "contractorName": "ABC Steelworks",
          |    "ERN": "123/AB123456",
          |    "deductionPeriods": [
          |      {
          |        "month": "MAY",
          |        "labour": 450,
          |        "cisDeduction": 100,
          |        "costOfMaterialsQuestion": true,
          |        "materialsCost": 50
          |      }
          |    ],
          |    "customerDeductionPeriods": [
          |      {
          |        "month": "MAY",
          |        "labour": 450,
          |        "cisDeduction": 100,
          |        "costOfMaterialsQuestion": true,
          |        "materialsCost": 50
          |      }
          |    ]
          |  },
          |  "newContractor": {
          |    "contractorName": "ABC Steelworks",
          |    "ERN": "123/AB123456",
          |    "deductionPeriods": [
          |      {
          |        "month": "NOVEMBER",
          |        "labour": 500,
          |        "cisDeduction": 100,
          |        "costOfMaterialsQuestion": true,
          |        "materialsCost": 250
          |      }
          |    ],
          |    "customerDeductionPeriods": {
          |        "month": "MAY",
          |        "labour": 500,
          |        "cisDeduction": 100,
          |        "costOfMaterialsQuestion": true,
          |        "materialsCost": 250
          |    }
          |  }
          |}
          |""".stripMargin)

      val amendCisContractorPayload = AmendCisContractorPayload(employerRef = aCisUserData.employerRef, cisUserData = aCisUserData, incomeTaxUserData = anIncomeTaxUserData)

      Json.toJson(amendCisContractorPayload) shouldBe json
    }
  }
}
