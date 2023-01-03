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

package actions

import play.api.mvc.Results.InternalServerError
import support.UnitTest
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.mocks.MockErrorHandler

import scala.concurrent.ExecutionContext

class MonthFilterActionSpec extends UnitTest
  with MockErrorHandler {

  private val monthValue = "MaY"

  private val executionContext = ExecutionContext.global

  ".executionContext" should {
    "return the given execution context" in {
      val underTest = MonthFilterAction(monthValue = monthValue, errorHandler = mockErrorHandler)(executionContext)

      underTest.executionContext shouldBe executionContext
    }
  }

  ".filter" should {
    "return a redirect to errorHandler.internalServerError if Month not a valid value" in {
      val underTest = MonthFilterAction(monthValue = "wrong-month-value", errorHandler = mockErrorHandler)(executionContext)
      mockInternalServerError(InternalServerError)

      await(underTest.filter(anAuthorisationRequest)) shouldBe Some(InternalServerError)
    }

    "return None when month is a correct value" in {
      val underTest = MonthFilterAction(monthValue = monthValue, errorHandler = mockErrorHandler)(executionContext)

      await(underTest.filter(anAuthorisationRequest)) shouldBe None
    }
  }
}
