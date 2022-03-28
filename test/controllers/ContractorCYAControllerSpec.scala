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

package controllers

import play.api.http.Status.OK
import play.api.test.Helpers.{contentType, status}
import support.ControllerUnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.mocks.MockActionsProvider
import views.html.ContractorCYAView

import java.time.Month

class ContractorCYAControllerSpec extends ControllerUnitTest
  with MockActionsProvider {

  private val pageView = inject[ContractorCYAView]

  private val underTest = new ContractorCYAController(mockActionsProvider, pageView)

  ".show" should {
    "return successful response" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "12345", periodData = Seq(aPeriodData.copy(deductionPeriod = Month.MAY)))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      mockInYearWithPreviousDataFor(taxYear, month = "may", contractor = "12345", anIncomeTaxUserData.copy(cis = Some(allCISDeductions)))

      val result = underTest.show(taxYear, Month.MAY.toString.toLowerCase, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }
}
