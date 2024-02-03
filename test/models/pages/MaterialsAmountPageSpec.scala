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
import support.builders.models.pages.MaterialsAmountPageBuilder.aMaterialsAmountPage

import java.time.Month

class MaterialsAmountPageSpec extends UnitTest {

  private val formsProvider = new FormsProvider()

  "MaterialsAmountPage.isReplay" should {
    "return true when originalAmount is defined" in {
      val underTest = aMaterialsAmountPage.copy(originalAmount = Some(200.00))

      underTest.isReplay shouldBe true
    }

    "return false when originalAmount isn't defined" in {
      val underTest = aMaterialsAmountPage.copy(originalAmount = None)

      underTest.isReplay shouldBe false
    }
  }

  "MaterialsAmountPage.apply" should {
    "return MaterialsAmountPage with pre-filled form when costOfMaterials exists" in {
      val cyaPeriodData = aCYAPeriodData.copy(costOfMaterials = Some(123.45))
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      MaterialsAmountPage.apply(Month.JUNE, cisUserData, formsProvider.materialsAmountForm(isAgent = false)) shouldBe MaterialsAmountPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.materialsAmountForm(isAgent = false).fill(value = 123.45),
        originalAmount = Some(123.45)
      )
    }

    "return MaterialsAmountPage without pre-filled form when costOfMaterials is not present" in {
      val cyaPeriodData = aCYAPeriodData.copy(costOfMaterials = None)
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      MaterialsAmountPage.apply(Month.JUNE, cisUserData, formsProvider.materialsAmountForm(isAgent = true)) shouldBe MaterialsAmountPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.materialsAmountForm(isAgent = true),
        originalAmount = None
      )
    }

    "return MaterialsAmountPage with pre-filled form with errors" in {
      val cyaPeriodData = aCYAPeriodData.copy(costOfMaterials = Some(123.45))
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      MaterialsAmountPage.apply(Month.JUNE, cisUserData, formsProvider.materialsAmountForm(isAgent = false).bind(Map("amount" -> "wrong-amount"))) shouldBe MaterialsAmountPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.materialsAmountForm(isAgent = false).bind(Map("amount" -> "wrong-amount")),
        originalAmount = Some(123.45)
      )
    }
  }
}
