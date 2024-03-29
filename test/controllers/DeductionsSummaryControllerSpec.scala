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

package controllers

import play.api.http.Status.OK
import play.api.test.Helpers.{contentType, status}
import support.ControllerUnitTest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.mocks.{MockActionsProvider, MockErrorHandler, MockTailoringService}
import utils.InYearUtil
import views.html.DeductionsSummaryView

class DeductionsSummaryControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockTailoringService
  with MockErrorHandler {

  private val pageView = inject[DeductionsSummaryView]

  private val underTest = new DeductionsSummaryController(mockActionsProvider, new InYearUtil(), pageView, mockTailoringService, mockErrorHandler)

  ".show" should {
    "return successful response when in year" in {
      mockPriorCisDeductionsData(taxYear, anIncomeTaxUserData)

      val result = underTest.show(taxYear).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }

    "return successful response when end of the year" in {
      mockPriorCisDeductionsData(taxYearEOY, anIncomeTaxUserData)

      val result = underTest.show(taxYearEOY).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }
}
