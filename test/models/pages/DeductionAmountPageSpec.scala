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

package models.pages

import forms.FormsProvider
import support.UnitTest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.builders.models.pages.DeductionAmountPageBuilder.aDeductionAmountPage

import java.time.Month

class DeductionAmountPageSpec extends UnitTest {

  private val formsProvider = new FormsProvider()

  "DeductionAmountPage.isReplay" should {
    "return true when originalAmount exists" in {
      val underTest = aDeductionAmountPage.copy(originalAmount = Some(123.01))

      underTest.isReplay shouldBe true
    }

    "return true when originalAmount does not exists" in {
      val underTest = aDeductionAmountPage.copy(originalAmount = None)

      underTest.isReplay shouldBe false
    }
  }

  "DeductionAmountPage.apply" should {
    "return DeductionAmountPage with pre-filled form when deductionAmount is present" in {
      val cyaPeriodData = aCYAPeriodData.copy(deductionAmount = Some(123.45))
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      DeductionAmountPage.apply(Month.JUNE, cisUserData, formsProvider.deductionAmountForm()) shouldBe DeductionAmountPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.deductionAmountForm().fill(value = 123.45),
        originalAmount = Some(123.45)
      )
    }

    "return DeductionAmountPage without pre-filled form when deductionAmount is not present" in {
      val cyaPeriodData = aCYAPeriodData.copy(deductionAmount = None)
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      DeductionAmountPage.apply(Month.JUNE, cisUserData, formsProvider.deductionAmountForm()) shouldBe DeductionAmountPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.labourPayAmountForm(isAgent = true),
        originalAmount = None
      )
    }

    "return DeductionAmountPage with pre-filled form with errors form has errors" in {
      val cyaPeriodData = aCYAPeriodData.copy(deductionAmount = Some(123.45))
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      DeductionAmountPage.apply(Month.JUNE, cisUserData, formsProvider.deductionAmountForm().bind(Map("amount" -> "wrong-amount"))) shouldBe DeductionAmountPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.deductionAmountForm().bind(Map("amount" -> "wrong-amount")),
        originalAmount = Some(123.45)
      )
    }
  }
}
