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

import models._
import models.mongo.{CYAPeriodData, DataNotFoundError, DataNotUpdatedError}
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.MockCISSessionService
import support.{TaxYearHelper, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class LabourPayServiceSpec extends UnitTest
  with MockCISSessionService
  with TaxYearHelper {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val underTest = new LabourPayService(mockCISSessionService)

  ".saveLabourPay" should {
    "return error when CISSessionService.getSessionData errors with any DatabaseError" in {
      mockGetSessionData(taxYear, aUser, employerRef = "some-ref", Left(DataNotFoundError))

      await(underTest.saveLabourPay(taxYear, employerRef = "some-ref", user = aUser, amount = 123)) shouldBe Left(DataNotFoundError)
    }

    "return NoCisUserDataError when CISSessionService.getSessionData returns None" in {
      mockGetSessionData(taxYear, aUser, employerRef = "some-ref", Right(None))

      await(underTest.saveLabourPay(taxYear, employerRef = "some-ref", aUser, amount = 123)) shouldBe Left(NoCisUserDataError)
    }

    "return NoCYAPeriodDataError when CISSessionService.getSessionData returns cisUserData without PeriodData" in {
      val cisUserDataWithoutPeriodData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))
      mockGetSessionData(taxYear, aUser, employerRef = "some-ref", Right(Some(cisUserDataWithoutPeriodData)))

      await(underTest.saveLabourPay(taxYear, employerRef = "some-ref", aUser, amount = 123)) shouldBe Left(NoCYAPeriodDataError)
    }

    "return DataNotUpdatedError when cisSessionService.createOrUpdateCISUserData returns error" in {
      val periodData: CYAPeriodData = aCYAPeriodData.copy(grossAmountPaid = Some(123))
      val updatedCYA = aCisUserData.cis.copy(periodData = Some(periodData))

      mockGetSessionData(taxYear, aUser, employerRef = "some-ref", Right(Some(aCisUserData)))
      mockCreateOrUpdateCISUserData(taxYear, aUser, "some-ref", aCisUserData.submissionId, aCisUserData.isPriorSubmission, updatedCYA, Left(aCisUserData))

      await(underTest.saveLabourPay(taxYear, employerRef = "some-ref", aUser, amount = 123)) shouldBe Left(DataNotUpdatedError)
    }

    "persist cisUserData when CISSessionService.getSessionData returns CisUserData and PeriodData exists" in {
      val periodData: CYAPeriodData = aCYAPeriodData.copy(grossAmountPaid = Some(123))
      val updatedCYA = aCisUserData.cis.copy(periodData = Some(periodData))

      mockGetSessionData(taxYear, aUser, employerRef = "some-ref", Right(Some(aCisUserData)))
      mockCreateOrUpdateCISUserData(taxYear, aUser, "some-ref", aCisUserData.submissionId, aCisUserData.isPriorSubmission, updatedCYA, Right(aCisUserData))

      await(underTest.saveLabourPay(taxYear, employerRef = "some-ref", aUser, amount = 123)) shouldBe Right(())
    }
  }
}
