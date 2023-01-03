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
import support.builders.models.audit.CreateNewCisContractorBuilder.aCreateNewCisContractor
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

class CreateNewCisContractorSpec extends UnitTest {

  ".mapFrom" should {
    "return a CreateNewCisContractor case class when periodData is defined" in {
      CreateNewCisContractor.mapFrom(aCisUserData.employerRef, aCisCYAModel) shouldBe Some(aCreateNewCisContractor)
    }
    "return None when periodData is None" in {
      CreateNewCisContractor.mapFrom(aCisUserData.employerRef, aCisCYAModel.copy(periodData = None)) shouldBe None
    }
  }

  ".writes" should {
    "produce a valid json when passed CreateNewCisContractor" in {
      val json = Json.parse(
        """
          |{
          |  "contractorName": "ABC Steelworks",
          |  "ern": "123/AB123456",
          |  "customerDeductionPeriod": {
          |    "month": "MAY",
          |    "labour": 500,
          |    "cisDeduction": 100,
          |    "paidForMaterials": true,
          |    "materialsCost": 250
          |  }
          |}
          |""".stripMargin)

      Json.toJson(aCreateNewCisContractor) shouldBe json
    }
  }
}
