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

package services

import models.mongo.DataNotUpdatedError
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.UserBuilder._
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.MockCISSessionService
import support.{TaxYearProvider, UnitTest}

import scala.concurrent.ExecutionContext.Implicits.global

class ContractorCYAServiceSpec extends UnitTest
  with MockCISSessionService
  with TaxYearProvider {

  private val underTest = new ContractorCYAService(mockCISSessionService)

  ".submitCisDeductionCYA" should {
    "return right when clear is successful" in {

      mockClear(taxYear, aCisUserData.employerRef, result = Right(()))

      await(underTest.submitCisDeductionCYA(taxYear, aCisDeductions.employerRef, aUser)) shouldBe Right(())
    }
    "return error when clear fails" in {

      mockClear(taxYear, aCisUserData.employerRef, result = Left(DataNotUpdatedError))

      await(underTest.submitCisDeductionCYA(taxYear, aCisDeductions.employerRef, aUser)) shouldBe Left(DataNotUpdatedError)
    }
  }
}
