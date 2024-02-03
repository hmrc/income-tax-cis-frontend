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
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

class ViewCisPeriodPayloadSpec extends UnitTest {

  "apply" should {
    "return an ViewCisPeriodPayload" in {
      val priorData = aCisUserData.cis.periodData.get
      val contractorName = aCisUserData.cis.contractorName
      val employerRef = aCisUserData.employerRef
      val deductionPeriod = DeductionPeriod(
        month = priorData.deductionPeriod.toString,
        labour = priorData.grossAmountPaid,
        cisDeduction = priorData.deductionAmount,
        costOfMaterialsQuestion = priorData.costOfMaterialsQuestion.get,
        materialsCost = priorData.costOfMaterials)

      val viewCisPeriodPayload = ViewCisPeriodPayload(
        ContractorDetails(
          name = contractorName,
          ern = employerRef),
          deductionPeriod)

      contractorName shouldBe viewCisPeriodPayload.contractorDetails.name
      employerRef shouldBe viewCisPeriodPayload.contractorDetails.ern
      deductionPeriod shouldBe viewCisPeriodPayload.customerDeductionPeriod
    }
  }

  "writes" should {
    "produce valid json when passed a ViewCisContractorPayload" in {
      val json = Json.parse(
        """
          |{
          |  "cisPeriod": {
          |    "name": "ABC Steelworks",
          |    "ERN": "123/AB123456",
          |    "month": "MAY",
          |    "labour": 500,
          |    "cisDeduction": 100,
          |    "costOfMaterialsQuestion": true,
          |    "materialsCost": 250
          |  }
          |}
          |""".stripMargin)

      val priorData = aCisUserData.cis.periodData.get
      val contractorName = aCisUserData.cis.contractorName
      val employerRef = aCisUserData.employerRef
      val viewCisPeriodPayload = ViewCisPeriodPayload(
        ContractorDetails(
          name = contractorName,
          ern = employerRef),
        DeductionPeriod(
          month = priorData.deductionPeriod.toString,
          labour = priorData.grossAmountPaid,
          cisDeduction = priorData.deductionAmount,
          costOfMaterialsQuestion = priorData.costOfMaterialsQuestion.get,
          materialsCost = priorData.costOfMaterials))

      Json.toJson(viewCisPeriodPayload) shouldBe json
    }
  }
}
