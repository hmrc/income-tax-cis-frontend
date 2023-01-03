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

package services

import models.mongo.{CYAPeriodData, DataNotUpdatedError}
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.MockCISSessionService
import support.{TaxYearProvider, UnitTest}

import scala.concurrent.ExecutionContext.Implicits.global

class MaterialsServiceSpec extends UnitTest
  with MockCISSessionService
  with TaxYearProvider {

  private val underTest = new MaterialsService(mockCISSessionService)

  ".saveQuestion" should {
    "return DataNotUpdatedError when cisSessionService.createOrUpdateCISUserData returns error" in {
      val periodData: CYAPeriodData = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(true))
      val updatedCYA = aCisUserData.cis.copy(periodData = Some(periodData))

      mockCreateOrUpdateCISUserData(aCisUserData.taxYear, aUser, aCisUserData.employerRef, aCisUserData.submissionId, aCisUserData.isPriorSubmission, updatedCYA, Left(DataNotUpdatedError))

      await(underTest.saveQuestion(aUser, aCisUserData, question = true)) shouldBe Left(DataNotUpdatedError)
    }

    "persist cisUserData when question is Yes" in {
      val periodData: CYAPeriodData = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(true))
      val updatedCYA = aCisUserData.cis.copy(periodData = Some(periodData))

      mockCreateOrUpdateCISUserData(aCisUserData.taxYear, aUser, aCisUserData.employerRef, aCisUserData.submissionId, aCisUserData.isPriorSubmission, updatedCYA, Right(aCisUserData))

      await(underTest.saveQuestion(aUser, aCisUserData, question = true)) shouldBe Right(aCisUserData)
    }

    "persist cisUserData when question is No" in {
      val periodData: CYAPeriodData = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(false), costOfMaterials = None)
      val updatedCYA = aCisUserData.cis.copy(periodData = Some(periodData))

      mockCreateOrUpdateCISUserData(aCisUserData.taxYear, aUser, aCisUserData.employerRef, aCisUserData.submissionId, aCisUserData.isPriorSubmission, updatedCYA, Right(aCisUserData))

      await(underTest.saveQuestion(aUser, aCisUserData, question = false)) shouldBe Right(aCisUserData)
    }
  }

  ".saveAmount" should {
    "return DataNotUpdatedError when cisSessionService.createOrUpdateCISUserData returns error" in {
      val periodData: CYAPeriodData = aCYAPeriodData.copy(costOfMaterials = Some(123))
      val updatedCYA = aCisUserData.cis.copy(periodData = Some(periodData))

      mockCreateOrUpdateCISUserData(aCisUserData.taxYear, aUser, aCisUserData.employerRef, aCisUserData.submissionId, aCisUserData.isPriorSubmission, updatedCYA, Left(DataNotUpdatedError))

      await(underTest.saveAmount(aUser, aCisUserData, amount = 123)) shouldBe Left(DataNotUpdatedError)
    }

    "persist cisUserData when" in {
      val periodData: CYAPeriodData = aCYAPeriodData.copy(costOfMaterials = Some(123))
      val updatedCYA = aCisUserData.cis.copy(periodData = Some(periodData))

      mockCreateOrUpdateCISUserData(aCisUserData.taxYear, aUser, aCisUserData.employerRef, aCisUserData.submissionId, aCisUserData.isPriorSubmission, updatedCYA, Right(aCisUserData))

      await(underTest.saveAmount(aUser, aCisUserData, amount = 123)) shouldBe Right(())
    }
  }
}
