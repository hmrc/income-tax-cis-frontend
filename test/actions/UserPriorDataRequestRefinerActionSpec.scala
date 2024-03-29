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

package actions

import models.{HttpParserError, UserPriorDataRequest}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.InternalServerError
import support.UnitTest
import support.builders.models.AuthorisationRequestBuilder.anAuthorisationRequest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.mocks.{MockCISSessionService, MockErrorHandler}

import scala.concurrent.ExecutionContext

class UserPriorDataRequestRefinerActionSpec extends UnitTest
  with MockCISSessionService
  with MockErrorHandler {

  private val anyTaxYear = 2022
  private val executionContext = ExecutionContext.global

  private val underTest = UserPriorDataRequestRefinerAction(
    taxYear = anyTaxYear,
    cisSessionService = mockCISSessionService,
    errorHandler = mockErrorHandler,
  )(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".refine" should {
    "handle InternalServerError when when getting session data result in database error" in {
      mockGetPriorData(anyTaxYear, anAuthorisationRequest.user, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      await(underTest.refine(anAuthorisationRequest)) shouldBe Left(InternalServerError)
    }

    "return UserSessionDataRequest when period data exists" in {
      mockGetPriorData(anyTaxYear, anAuthorisationRequest.user, Right(anIncomeTaxUserData))

      await(underTest.refine(anAuthorisationRequest)) shouldBe Right(UserPriorDataRequest(anIncomeTaxUserData, anAuthorisationRequest.user, anAuthorisationRequest.request))
    }
  }
}
