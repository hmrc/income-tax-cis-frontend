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
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.audit.ContractorDetailsAndPeriodDataBuilder.aContractorDetailsAndPeriodData
import support.builders.models.audit.ViewCisPeriodAuditBuilder.aViewCisPeriodAudit
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.TaxYearUtils.{taxYear, taxYearEOY}

import java.time.Month

class ViewCisPeriodAuditSpec extends UnitTest {

  ".mapFrom(taxYear, user, deductions, deductionMonth)" should {
    "return a ViewCisPeriodAudit case class when cisPeriod is defined" in {
      ViewCisPeriodAudit.mapFrom(
        taxYear = taxYear,
        user = aUser,
        deductions = aCisDeductions,
        deductionMonth = aPeriodData.deductionPeriod) shouldBe Some(aViewCisPeriodAudit)
    }
    "return None when cisPeriod is None" in{
      ViewCisPeriodAudit.mapFrom(
        taxYear = taxYear,
        user = aUser,
        deductions = aCisDeductions.copy(periodData = Seq.empty),
        deductionMonth = aPeriodData.deductionPeriod) shouldBe None
    }
  }

    ".mapFrom(taxYear, user, cisUserData)" should {
      "return a ViewCisPeriodAudit case class when cisPeriod is defined" in {
        ViewCisPeriodAudit.mapFrom(
          taxYear = taxYearEOY,
          user = aUser,
          cisUserData = aCisUserData) shouldBe Some(aViewCisPeriodAudit.copy(taxYear = taxYearEOY,
            cisPeriod = aContractorDetailsAndPeriodData.copy(labour = Some(500), materialsCost = Some(250))
        ))
      }
      "return None when cisPeriod is None" in {
        ViewCisPeriodAudit.mapFrom(
          taxYear = taxYearEOY,
          user = aUser,
          cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))) shouldBe None
      }
    }

  "writes" should {
    "produce valid json when passed a ViewCisPeriodAudit" in {
      val cisPeriodJson = Json.parse(
        """
          |{
          |  "taxYear": 2023,
          |  "userType": "individual",
          |  "nino": "AA123456A",
          |  "mtditid": "1234567890",
          |  "cisPeriod": {
          |    "name": "ABC Steelworks",
          |    "ern": "123/AB123456",
          |    "month": "MAY",
          |    "labour": 100,
          |    "cisDeduction": 200,
          |    "paidForMaterials": true,
          |    "materialsCost": 300
          |  }
          |}
          |""".stripMargin
      )

      val audit = ViewCisPeriodAudit.mapFrom(
        taxYear = 2023,
        user = aUser,
        deductions = aCisDeductions.copy(periodData = Seq(aPeriodData.copy(
          grossAmountPaid = Some(100),
          deductionAmount = Some(200),
          costOfMaterials = Some(300)))),
        deductionMonth = Month.MAY)

      Json.toJson(audit) shouldBe cisPeriodJson
    }
  }
}
