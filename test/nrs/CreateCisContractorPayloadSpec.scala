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

package nrs

import models.nrs.CreateCisContractorPayload
import play.api.libs.json.Json
import support.UnitTest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import java.time.Month.NOVEMBER

class CreateCisContractorPayloadSpec extends UnitTest {

  "writes" should {
    "produce valid json when passed a CreateCisContractorPayload" in {
      val json = Json.parse(
        """
          |{
          |  "contractor": {
          |    "contractorName": "ABC Steelworks",
          |    "ERN": "123/AB123456",
          |    "customerDeductionPeriod": {
          |        "month": "NOVEMBER",
          |        "labour": 500,
          |        "cisDeduction": 100,
          |        "costOfMaterialsQuestion": true,
          |        "materialsCost": 250
          |    }
          |  }
          |}
          |""".stripMargin)

      val cisData = aCisUserData.copy(submissionId = None, cis = aCisCYAModel.copy(
        periodData = Some(aCYAPeriodData.copy(deductionPeriod = NOVEMBER)),
        priorPeriodData = Seq()
      ))

      val createCisContractorPayload = CreateCisContractorPayload.mapFrom(employerRef = cisData.employerRef, cisCYAModel = cisData.cis).get
      Json.toJson(createCisContractorPayload) shouldBe json
    }
  }
}
