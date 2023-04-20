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
import support.TaxYearUtils.taxYearEOY
import support.UnitTest
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.audit.DeleteCisPeriodAuditBuilder.aDeleteCisPeriodAudit

class DeleteCisPeriodAuditSpec extends UnitTest {

  ".apply" should {
    "return a DeleteCisPeriodAudit" in {
      DeleteCisPeriodAudit.apply(
        taxYear = taxYearEOY,
        user = aUser,
        contractorName = Some("ABC Steelworks"),
        employerRef = "123/AB123456",
        periodData = aPeriodData
      ) shouldBe aDeleteCisPeriodAudit
    }
  }

  "writes" should {
    "produce valid json when passed a DeleteCisPeriodAudit" in {
      val json = Json.parse(
        s"""
           |{
           |  "taxYear": $taxYearEOY,
           |  "userType": "individual",
           |  "nino": "AA123456A",
           |  "mtditid": "1234567890",
           |  "deletedCisPeriod": {
           |    "contractorDetails": {
           |      "name": "ABC Steelworks",
           |      "ern": "123/AB123456"
           |    },
           |    "month": "MAY",
           |    "labour": 450,
           |    "cisDeduction" : 100,
           |    "paidForMaterials": true,
           |    "materialsCost": 50
           |  }
           |}
           |""".stripMargin
      )

      Json.toJson(aDeleteCisPeriodAudit) shouldBe json
    }
  }
}
