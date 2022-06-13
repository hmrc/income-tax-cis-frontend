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

import controllers.routes.ContractorSummaryController
import models.{HttpParserError, InvalidOrUnfinishedSubmission}
import models.mongo.DataNotUpdatedError
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER, SERVICE_UNAVAILABLE}
import play.api.mvc.Results.{InternalServerError, ServiceUnavailable}
import play.api.test.Helpers.{contentType, redirectLocation, status}
import support.ControllerUnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockActionsProvider, MockContractorCYAService, MockErrorHandler}
import utils.InYearUtil
import views.html.ContractorCYAView

import java.time.Month

class ContractorCYAControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockContractorCYAService
  with MockErrorHandler {

  private val pageView = inject[ContractorCYAView]

  private val underTest = new ContractorCYAController(
    mockActionsProvider,
    pageView,
    new InYearUtil(),
    mockContractorCYAService,
    mockErrorHandler,
  )

  ".show" should {
    "return successful response" in {
      val cisDeductions = aCisDeductions.copy(employerRef = "12345", periodData = Seq(aPeriodData.copy(deductionPeriod = Month.MAY)))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(cisDeductions))))

      mockUserPriorDataFor(taxYear, contractor = "12345", month = "may", anIncomeTaxUserData.copy(cis = Some(allCISDeductions)))

      val result = underTest.show(taxYear, Month.MAY.toString.toLowerCase, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }

    "return successful response for end of year" in {

      mockCheckCyaExistsAndReturnSessionData(taxYearEOY, contractor = "12345", month = "may", aCisUserData)

      val result = underTest.show(taxYearEOY, Month.MAY.toString.toLowerCase, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {

    "return successful response for end of year" in {

      mockCheckCyaExistsAndReturnSessionData(taxYearEOY, contractor = "12345", month = "may", aCisUserData)
      mockSubmitCisDeductionCYA(taxYearEOY, "12345", Right(()))

      val result = underTest.submit(taxYearEOY, Month.MAY.toString.toLowerCase, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe ContractorSummaryController.show(taxYearEOY, "12345").url
    }
    "handle database error" in {

      mockCheckCyaExistsAndReturnSessionData(taxYearEOY, contractor = "12345", month = "may", aCisUserData)
      mockSubmitCisDeductionCYA(taxYearEOY, "12345", Left(DataNotUpdatedError))
      mockInternalError(InternalServerError)

      val result = underTest.submit(taxYearEOY, Month.MAY.toString.toLowerCase, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
    "handle not finished error" in {

      mockCheckCyaExistsAndReturnSessionData(taxYearEOY, contractor = "12345", month = "may", aCisUserData)
      mockSubmitCisDeductionCYA(taxYearEOY, "12345", Left(InvalidOrUnfinishedSubmission))

      val result = underTest.submit(taxYearEOY, Month.MAY.toString.toLowerCase, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.ContractorCYAController.show(taxYearEOY, Month.MAY.toString.toLowerCase,"12345").url
    }
    "handle parser error" in {

      mockCheckCyaExistsAndReturnSessionData(taxYearEOY, contractor = "12345", month = "may", aCisUserData)
      mockSubmitCisDeductionCYA(taxYearEOY, "12345", Left(HttpParserError(SERVICE_UNAVAILABLE)))
      mockHandleError(SERVICE_UNAVAILABLE, ServiceUnavailable)

      val result = underTest.submit(taxYearEOY, Month.MAY.toString.toLowerCase, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe SERVICE_UNAVAILABLE
    }
  }
}
