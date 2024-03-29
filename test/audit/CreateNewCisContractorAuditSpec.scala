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
import support.TaxYearUtils.taxYearEOY
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.audit.CreateNewCisContractorAuditBuilder.aCreateNewCisContractorAudit
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

class CreateNewCisContractorAuditSpec extends UnitTest {

  "writes" should {
    "produce valid json when passed a CreateNewCisContractorAudit" in {
      val json = Json.parse(
        s"""
          |{
          |  "taxYear": $taxYearEOY,
          |  "userType": "individual",
          |  "nino": "AA123456A",
          |  "mtditid": "1234567890",
          |  "contractor": {
          |    "contractorName": "ABC Steelworks",
          |    "ern": "123/AB123456",
          |    "customerDeductionPeriod": {
          |        "month": "MAY",
          |        "labour": 500,
          |        "cisDeduction": 100,
          |        "paidForMaterials": true,
          |        "materialsCost": 250
          |      }
          |  }
          |}
          |""".stripMargin
      )

      Json.toJson(aCreateNewCisContractorAudit) shouldBe json
    }
  }

  ".mapFrom" should {
    "return a CreateNewCisContractorAudit when contractor is defined" in {
      CreateNewCisContractorAudit.mapFrom(
        taxYear = taxYearEOY,
        employerRef = aCisUserData.employerRef,
        user = aUser,
        cisUserData = aCisUserData
      ) shouldBe Some(aCreateNewCisContractorAudit)
    }
    "return None when contractor is None" in {
      CreateNewCisContractorAudit.mapFrom(
        taxYear = taxYearEOY,
        employerRef = aCisUserData.employerRef,
        user = aUser,
        cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))
      ) shouldBe None
    }
  }
}
