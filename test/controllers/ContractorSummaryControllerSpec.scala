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

package controllers

import models.mongo.DataNotUpdatedError
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{contentType, redirectLocation, status}
import support.ControllerUnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.mocks.{MockActionsProvider, MockContractorSummaryService, MockErrorHandler}
import utils.InYearUtil
import views.html.ContractorSummaryView

class ContractorSummaryControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockContractorSummaryService
  with MockErrorHandler {

  private val pageView = inject[ContractorSummaryView]

  private val controller = new ContractorSummaryController(
    mockActionsProvider,
    pageView,
    new InYearUtil(),
    mockContractorSummaryService,
    mockErrorHandler
  )

  "show" should {
    "return a successful response" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "12345")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      mockUserPriorDataFor(taxYear, contractor = "12345", anIncomeTaxUserData.copy(cis = Some(allCISDeductions)))

      val result = controller.show(taxYear, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  "addCisDeduction" should {
    "return a successful response" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "12345")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      mockUserPriorDataFor(taxYearEOY, contractor = "12345", anIncomeTaxUserData.copy(cis = Some(allCISDeductions)))
      mockSaveCYAForNewCisDeduction(taxYearEOY, "12345", Right(()))

      val result = controller.addCisDeduction(taxYearEOY, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.DeductionPeriodController.show(taxYearEOY,"12345").url
    }
    "handle an error" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "12345")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      mockUserPriorDataFor(taxYearEOY, contractor = "12345", anIncomeTaxUserData.copy(cis = Some(allCISDeductions)))
      mockSaveCYAForNewCisDeduction(taxYearEOY, "12345", Left(DataNotUpdatedError))
      mockInternalServerError(InternalServerError)

      val result = controller.addCisDeduction(taxYearEOY, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
