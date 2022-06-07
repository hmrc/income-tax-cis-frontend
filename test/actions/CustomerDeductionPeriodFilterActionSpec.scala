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

package actions

import config.MockAppConfig
import play.api.mvc.Results.Redirect
import support.UnitTest
import support.builders.models.UserSessionDataRequestBuilder.aUserSessionDataRequest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

import java.time.Month
import scala.concurrent.ExecutionContext

class CustomerDeductionPeriodFilterActionSpec extends UnitTest {

  private val anyTaxYear = 2022
  private val appConfig = new MockAppConfig().config()
  private val executionContext = ExecutionContext.global

  private val underTest = CustomerDeductionPeriodFilterAction(taxYear = anyTaxYear, appConfig = appConfig)(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".filter" should {
    "return a redirect to Income Tax Submission Overview when has no more deduction periods to submit for" in {
      val fullPeriodDataList = Month.values().map(month => aCYAPeriodData.copy(deductionPeriod = month))
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(priorPeriodData = fullPeriodDataList))

      await(underTest.filter(aUserSessionDataRequest.copy(cisUserData = cisUserData))) shouldBe Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(anyTaxYear)))
    }

    "return a redirect to Income Tax Submission Overview when contractorSubmitted is true" in {
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(aCYAPeriodData.copy(contractorSubmitted = true))))

      await(underTest.filter(aUserSessionDataRequest.copy(cisUserData = cisUserData))) shouldBe Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(anyTaxYear)))
    }

    "return None when more periods can be added and contractorSubmitted is not true" in {
      await(underTest.filter(aUserSessionDataRequest)) shouldBe None
    }
  }
}
