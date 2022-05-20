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
import play.api.mvc.Results.{InternalServerError, Redirect}
import support.UnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserPriorDataRequestBuilder.aUserPriorDataRequest
import support.mocks.MockErrorHandler

import java.time.Month
import scala.concurrent.ExecutionContext

class HasEoyDeductionsForEmployerRefAndMonthFilterActionSpec extends UnitTest
  with MockErrorHandler {

  private val anyTaxYear = 2022
  private val employerRef = "some-employer-ref"
  private val monthValue = "may"
  private val appConfig = new MockAppConfig().config()
  private val executionContext = ExecutionContext.global

  private val underTest = HasEoyDeductionsForEmployerRefAndMonthFilterAction(
    taxYear = anyTaxYear,
    employerRef = employerRef,
    monthValue = monthValue,
    errorHandler = mockErrorHandler,
    appConfig = appConfig
  )(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".filter" should {
    "return a redirect to " in {
      val underTest = HasEoyDeductionsForEmployerRefAndMonthFilterAction(
        taxYear = anyTaxYear,
        employerRef = employerRef,
        monthValue = "wrong-month-value",
        errorHandler = mockErrorHandler,
        appConfig = appConfig
      )(executionContext)

      mockInternalError(InternalServerError)

      await(underTest.filter(aUserPriorDataRequest)) shouldBe Some(InternalServerError)
    }

    "return a redirect to Income Tax Submission Overview when CIS data has no end of year CisDeductions with given employer ref and month" in {
      val periodData = aPeriodData.copy(deductionPeriod = Month.JUNE)
      val cisSource = aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(employerRef = employerRef, periodData = Seq(periodData))))
      val incomeTaxUserData = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(contractorCISDeductions = Some(cisSource))))

      await(underTest.filter(aUserPriorDataRequest.copy(incomeTaxUserData = incomeTaxUserData))) shouldBe Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(anyTaxYear)))
    }

    "return None when CIS data has end of year CisDeductions" in {
      val periodData = aPeriodData.copy(deductionPeriod = Month.MAY)
      val cisSource = aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(employerRef = employerRef, periodData = Seq(periodData))))
      val incomeTaxUserData = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(contractorCISDeductions = Some(cisSource))))

      await(underTest.filter(aUserPriorDataRequest.copy(incomeTaxUserData = incomeTaxUserData))) shouldBe None
    }
  }
}
