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
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import support.builders.models.audit.AmendCisContractorAuditBuilder.anAmendCisContractorAudit
import support.builders.models.audit.AmendedCisContractorBuilder.anAmendedCisContractor
import support.builders.models.audit.DeductionPeriodDataBuilder.aDeductionPeriodData
import support.builders.models.audit.PreviousCisContractorBuilder.aPreviousCisContractor
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

import java.time.Month

class AmendCisContractorAuditSpec extends UnitTest {

  "writes" should {
    "produce valid json when passed a AmendCisContractorAudit" in {
      val json = Json.parse(
        """
          |{
          |  "taxYear": 2022,
          |  "userType": "individual",
          |  "nino": "AA123456A",
          |  "mtditid": "1234567890",
          |  "previousContractor": {
          |    "contractorName": "ABC Steelworks",
          |    "ern": "123/AB123456",
          |    "deductionPeriods": [
          |      {
          |        "month": "MAY",
          |        "labour": 450,
          |        "cisDeduction": 100,
          |        "paidForMaterials": true,
          |        "materialsCost": 50
          |      }
          |    ],
          |    "customerDeductionPeriods": [{
          |        "month": "JUNE",
          |        "labour": 300,
          |        "cisDeduction": 200,
          |        "paidForMaterials": true,
          |        "materialsCost": 100
          |      }]
          |  },
          |  "newContractor": {
          |    "contractorName": "ABC Steelworks",
          |    "ern": "123/AB123456",
          |    "deductionPeriods": [
          |      {
          |        "month": "MAY",
          |        "labour": 500,
          |        "cisDeduction": 100,
          |        "paidForMaterials": true,
          |        "materialsCost": 250
          |      }
          |    ],
          |    "customerDeductionPeriods": {
          |        "month": "JULY",
          |        "labour": 500,
          |        "cisDeduction": 100,
          |        "paidForMaterials": false
          |      }
          |  }
          |}
          |""".stripMargin)

      val audit = anAmendCisContractorAudit.copy(
        previousContractor = aPreviousCisContractor.copy(
          customerDeductionPeriods =
            Seq(aDeductionPeriodData.copy(month = Month.JUNE.toString, labour = Some(300), cisDeduction = Some(200), materialsCost = Some(100)))
        ),
        newContractor = anAmendedCisContractor.copy(
          deductionPeriods = Seq(aDeductionPeriodData.copy(month = Month.MAY.toString)),
          customerDeductionPeriods = Some(aDeductionPeriodData.copy(month = Month.JULY.toString, paidForMaterials = false, materialsCost = None))
        )
      )

      Json.toJson(audit) shouldBe json
    }
  }

  ".apply" should {
    "return an AmendCisContractorAudit case class" in {
      AmendCisContractorAudit.apply(
        taxYear = 2022,
        employerRef = aCisUserData.employerRef,
        user = aUser,
        cisUserData = aCisUserData,
        incomeTaxUserData = anIncomeTaxUserData
      ) shouldBe anAmendCisContractorAudit
    }
  }
}
