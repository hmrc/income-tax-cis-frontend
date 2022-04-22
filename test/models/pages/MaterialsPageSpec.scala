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

import forms.{FormsProvider, YesNoForm}
import support.UnitTest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

import java.time.Month

class MaterialsPageSpec extends UnitTest {

  private val formsProvider = new FormsProvider()

  "MaterialsPage.apply(...)" should {
    "return MaterialsPage with pre-filled form when costOfMaterialsQuestion is present" in {
      val cyaPeriodData = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(false))
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      val expectedPage = MaterialsPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.materialsYesNoForm(isAgent = true).fill(value = false)
      )

      val result = MaterialsPage.apply(Month.JUNE, cisUserData, formsProvider.materialsYesNoForm(isAgent = true))

      result.taxYear shouldBe expectedPage.taxYear
      result.month shouldBe expectedPage.month
      result.contractorName shouldBe expectedPage.contractorName
      result.employerRef shouldBe expectedPage.employerRef
      result.form.value shouldBe expectedPage.form.value
    }

    "return MaterialsPage without pre-filled form when costOfMaterialsQuestion is not present" in {
      val cyaPeriodData = aCYAPeriodData.copy(costOfMaterialsQuestion = None)
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      val expectedPage = MaterialsPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.materialsYesNoForm(isAgent = true)
      )

      val result = MaterialsPage.apply(Month.JUNE, cisUserData, formsProvider.materialsYesNoForm(isAgent = true))

      result.taxYear shouldBe expectedPage.taxYear
      result.month shouldBe expectedPage.month
      result.contractorName shouldBe expectedPage.contractorName
      result.employerRef shouldBe expectedPage.employerRef
      result.form.value shouldBe expectedPage.form.value
    }

    "return MaterialsPage with pre-filled form with errors form has errors" in {
      val cyaPeriodData = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(true))
      val cisUserData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData)))

      val expectedPage = MaterialsPage(
        taxYear = cisUserData.taxYear,
        month = Month.JUNE,
        contractorName = cisUserData.cis.contractorName,
        employerRef = cisUserData.employerRef,
        form = formsProvider.materialsYesNoForm(isAgent = true)
      )

      val result = MaterialsPage.apply(Month.JUNE, cisUserData, formsProvider.materialsYesNoForm(isAgent = true).bind(Map(YesNoForm.yesNo -> "")))

      result.taxYear shouldBe expectedPage.taxYear
      result.month shouldBe expectedPage.month
      result.contractorName shouldBe expectedPage.contractorName
      result.employerRef shouldBe expectedPage.employerRef
      result.form.value shouldBe expectedPage.form.value
    }
  }
}
