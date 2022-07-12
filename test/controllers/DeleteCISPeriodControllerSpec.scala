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

import models.HttpParserError
import models.mongo.DataNotFoundError
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{contentType, status}
import support.ControllerUnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import support.mocks.{MockActionsProvider, MockDeleteCISPeriodService}
import views.html.DeleteCISPeriodView

import java.time.Month.MAY

class DeleteCISPeriodControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockDeleteCISPeriodService {

  private val pageView: DeleteCISPeriodView = inject[DeleteCISPeriodView]

  private val underTest = new DeleteCISPeriodController(mockActionsProvider, pageView, mockErrorHandler, mockService)(cc, ec, appConfig)

  ".show" should {
    "return the page" in {
      mockExclusivelyCustomerPriorDataForEOY(taxYearEOY, aCisDeductions.employerRef, month = "may", anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions)))

      val result = underTest.show(taxYearEOY, month = "may", contractor = aCisDeductions.employerRef).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {
    "redirect when service returns Right" in {
      mockExclusivelyCustomerPriorDataForEOY(taxYearEOY, aCisDeductions.employerRef, month = "may", anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions)))
      mockRemoveCISDeduction(taxYearEOY, aCisDeductions.employerRef, aUser, MAY, anIncomeTaxUserData, Right(()))

      status(underTest.submit(taxYearEOY, aCisDeductions.employerRef, month = "may").apply(fakeIndividualRequest)) shouldBe SEE_OTHER
    }

    "throw an error when service returns Left" in {
      mockExclusivelyCustomerPriorDataForEOY(taxYearEOY, aCisDeductions.employerRef, month = "may", anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions)))
      mockRemoveCISDeduction(taxYearEOY, aCisDeductions.employerRef, aUser, MAY, anIncomeTaxUserData, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      status(underTest.submit(taxYearEOY, aCisDeductions.employerRef, month = "may").apply(fakeIndividualRequest)) shouldBe INTERNAL_SERVER_ERROR
    }

    "throw a internal server error when service returns Left" in {
      mockExclusivelyCustomerPriorDataForEOY(taxYearEOY, aCisDeductions.employerRef, month = "may", anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions)))
      mockRemoveCISDeduction(taxYearEOY, aCisDeductions.employerRef, aUser, MAY, anIncomeTaxUserData, Left(DataNotFoundError))
      mockInternalServerError(InternalServerError)

      status(underTest.submit(taxYearEOY, aCisDeductions.employerRef, month = "may").apply(fakeIndividualRequest)) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
