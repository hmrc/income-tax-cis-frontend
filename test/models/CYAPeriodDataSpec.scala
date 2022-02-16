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

package models

import builders.models.mongo.CYAPeriodDataBuilder.{aCYAPeriodData, aCYAPeriodDataJson}
import models.mongo.CYAPeriodData
import play.api.libs.json.Json
import utils.UnitTest

class CYAPeriodDataSpec extends UnitTest {

  "CYAPeriodData" should {

    "write to Json correctly when using implicit writes" in {
      val actualResult = Json.toJson(aCYAPeriodData)
      actualResult shouldBe aCYAPeriodDataJson
    }

    "read to Json correctly when using implicit read" in {
      val result = aCYAPeriodDataJson.as[CYAPeriodData]
      result shouldBe aCYAPeriodData
    }
  }

  "CYAPeriodData.isFinish" should {

    "return true" when {
      "grossAmountPaid.isDefined, deductionAmount.isDefined and costOfMaterialsQuestion is false" in {
        val result = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(false), costOfMaterials = None)
        result.isFinished shouldBe true
      }

      "grossAmountPaid.isDefined, deductionAmount.isDefined, costOfMaterials section is complete" in {
        aCYAPeriodData.isFinished shouldBe true
      }
    }

    "return false" when {
      "grossAmountPaid isn't define" in {
        val result = aCYAPeriodData.copy(grossAmountPaid = None)
        result.isFinished shouldBe false
      }

      "deductionAmount isn't defined" in {
        val result = aCYAPeriodData.copy(deductionAmount = None)
        result.isFinished shouldBe false
      }

      "costOfMaterialsQuestion is None" in {
        val result = aCYAPeriodData.copy(costOfMaterialsQuestion = None)
        result.isFinished shouldBe false
      }

      "costOfMaterialsQuestion is Some(true) and costOfMaterials is None" in {
        val result = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(true), costOfMaterials = None)
        result.isFinished shouldBe false
      }
    }
  }

}
