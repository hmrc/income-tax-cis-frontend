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

package models.pages

import forms.FormsProvider
import support.UnitTest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.builders.models.pages.LabourPayPageBuilder.aLabourPayPage

import java.time.Month

class LabourPayPageSpec extends UnitTest {

  private val formsProvider = new FormsProvider()

  "LabourPayPage.contractor" should {
    "return contractor name when exists" in {
      val underTest = aLabourPayPage.copy(contractorName = Some("some-contractor-name"))

      underTest.contractor shouldBe "some-contractor-name"
    }

    "return employerRef when contractor name does not exists" in {
      val underTest = aLabourPayPage.copy(contractorName = None, employerRef = "some-employer-ref")

      underTest.contractor shouldBe "some-employer-ref"
    }
  }

  "LabourPayPage.isReplay" should {
    "return true when originalGrossAmount exists" in {
      val underTest = aLabourPayPage.copy(originalGrossAmount = Some(123.01))

      underTest.isReplay shouldBe true
    }

    "return true when originalGrossAmount does not exists" in {
      val underTest = aLabourPayPage.copy(originalGrossAmount = None)

      underTest.isReplay shouldBe false
    }
  }

  "LabourPayPage.apply" should {
    "return LabourPayPage with pre-filled form when grossAmount is present" in {
      val cyaPeriodData = aCYAPeriodData.copy(grossAmountPaid = Some(123.45))
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      LabourPayPage.apply(Month.JUNE, cisUserData, formsProvider.labourPayAmountForm(isAgent = true)) shouldBe LabourPayPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.labourPayAmountForm(isAgent = true).fill(value = 123.45),
        originalGrossAmount = Some(123.45)
      )
    }

    "return LabourPayPage without pre-filled form when grossAmount is not present" in {
      val cyaPeriodData = aCYAPeriodData.copy(grossAmountPaid = None)
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      LabourPayPage.apply(Month.JUNE, cisUserData, formsProvider.labourPayAmountForm(isAgent = true)) shouldBe LabourPayPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.labourPayAmountForm(isAgent = true),
        originalGrossAmount = None
      )
    }

    "return LabourPayPage with pre-filled form with errors form has errors" in {
      val cyaPeriodData = aCYAPeriodData.copy(grossAmountPaid = Some(123.45))
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      LabourPayPage.apply(Month.JUNE, cisUserData, formsProvider.labourPayAmountForm(isAgent = true).bind(Map("amount" -> "wrong-amount"))) shouldBe LabourPayPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.labourPayAmountForm(isAgent = true).bind(Map("amount" -> "wrong-amount")),
        originalGrossAmount = Some(123.45)
      )
    }
  }
}
