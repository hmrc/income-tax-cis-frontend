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

package actions

import play.api.mvc.Results.Redirect
import support.UnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserPriorDataRequestBuilder.aUserPriorDataRequest
import support.stubs.AppConfigStub

import scala.concurrent.ExecutionContext

class HasInYearPeriodDataWithEmployerRefFilterActionSpec extends UnitTest {

  private val taxYear = 2022
  private val employerRef = "some-employer-ref"
  private val appConfig = new AppConfigStub().config()
  private val executionContext = ExecutionContext.global

  private val underTest = HasInYearPeriodDataWithEmployerRefFilterAction(
    taxYear = taxYear,
    employerRef = employerRef,
    appConfig = appConfig
  )(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".refine" should {
    "return a redirect to Income Tax Submission Overview when CIS data has no in year Period data with given employerRef" in {
      val deductions = aCisDeductions.copy(employerRef = "unknown-ref")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))
      val incomeTaxUserData = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      await(underTest.filter(aUserPriorDataRequest.copy(incomeTaxUserData = incomeTaxUserData))) shouldBe Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }

    "return None when CIS data contains CisDeductions with given employerRef" in {
      val deductions = aCisDeductions.copy(employerRef = employerRef)
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))
      val incomeTaxUserData = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      await(underTest.filter(aUserPriorDataRequest.copy(incomeTaxUserData = incomeTaxUserData))) shouldBe None
    }
  }
}
