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

package models.pages

import forms.DeductionPeriodFormProvider
import support.UnitTest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

import java.time.Month
import java.time.Month.{AUGUST, NOVEMBER}

class DeductionPeriodPageSpec extends UnitTest {

  private val anyTaxYear = 2022
  private val anyBoolean = true
  private val deductionPeriodFormProvider = new DeductionPeriodFormProvider()

  "DeductionPeriodPage.apply(taxYear: Int, cisUserData: CisUserData)" should {
    "return a DeductionPeriodPage" in {
      val cisCYAModel = aCisCYAModel.copy(
        contractorName = Some("some-contractor"),
        periodData = Some(aCYAPeriodData.copy(deductionPeriod = AUGUST)),
        priorPeriodData = Seq(aCYAPeriodData.copy(deductionPeriod = Month.NOVEMBER))
      )
      val cisUserData = aCisUserData.copy(cis = cisCYAModel, employerRef = "some-ref")
      val deductionPeriodForm = deductionPeriodFormProvider.deductionPeriodForm(isAgent = anyBoolean)

      DeductionPeriodPage.apply(anyTaxYear, cisUserData, deductionPeriodForm) shouldBe DeductionPeriodPage(
        taxYear = anyTaxYear,
        contractorName = Some("some-contractor"),
        employerRef = "some-ref",
        period = Some(AUGUST),
        priorSubmittedPeriods = Seq(NOVEMBER),
        form = deductionPeriodForm
      )
    }
  }
}
