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

package actions

import controllers.routes.{ContractorDetailsController, DeductionPeriodController, LabourPayController}
import models.mongo.CisUserData
import play.api.mvc.Results.Redirect
import support.TaxYearUtils.taxYearEOY
import support.UnitTest
import support.builders.models.UserSessionDataRequestBuilder.aUserSessionDataRequest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

import scala.concurrent.ExecutionContext

class CisUserDataFinishedFilterActionSpec extends UnitTest {

  private val executionContext = ExecutionContext.global

  private val underTest = CisUserDataFinishedFilterAction(taxYearEOY)(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".filter" should {
    "return None when gathering of cisUserData is finished" in {
      val userSessionDataWithAllDataCollected = aUserSessionDataRequest
      await(underTest.filter(userSessionDataWithAllDataCollected)) shouldBe None
    }

    "return a redirect to Contractor Details page when it is not PriorSubmission and any of the data is missing" in {
      val cisUserData: CisUserData = aCisUserData.copy(isPriorSubmission = false)
        .copy(cis = aCisCYAModel.copy(periodData = Some(aCYAPeriodData.copy(costOfMaterials = None))))

      await(underTest.filter(aUserSessionDataRequest.copy(cisUserData = cisUserData))) shouldBe
        Some(Redirect(ContractorDetailsController.show(taxYearEOY, Some(cisUserData.employerRef)).url))
    }

    "return a redirect to Deduction Period page when contractorSubmitted is false and any of the data is missing" in {
      val cisUserData: CisUserData = aCisUserData
        .copy(cis = aCisCYAModel.copy(periodData = Some(aCYAPeriodData.copy(contractorSubmitted = false, costOfMaterials = None))))

      await(underTest.filter(aUserSessionDataRequest.copy(cisUserData = cisUserData))) shouldBe
        Some(Redirect(DeductionPeriodController.show(taxYearEOY, cisUserData.employerRef).url))
    }

    "return a redirect to Labour pay page when contractorSubmitted is true and any of the data is missing" in {
      val cisUserData: CisUserData = aCisUserData
        .copy(cis = aCisCYAModel.copy(periodData = Some(aCYAPeriodData.copy(contractorSubmitted = true, costOfMaterials = None))))
      val month = cisUserData.cis.periodData.get.deductionPeriod.toString.toLowerCase()

      await(underTest.filter(aUserSessionDataRequest.copy(cisUserData = cisUserData))) shouldBe
        Some(Redirect(LabourPayController.show(taxYearEOY, month, cisUserData.employerRef).url))
    }
  }
}
