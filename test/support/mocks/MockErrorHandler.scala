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

package support.mocks

import config.ErrorHandler
import models.AuthorisationRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import play.api.mvc.{Request, Result}

trait MockErrorHandler { this: TestSuite =>


  protected val mockErrorHandler: ErrorHandler = org.mockito.Mockito.mock(classOf[ErrorHandler])

  def mockHandleError(status: Int, result: Result): Unit = {
    when(
      mockErrorHandler.handleError(org.mockito.ArgumentMatchers.eq(status))(any[Request[_]]())
    ).thenReturn(result)
  }

  def mockInternalServerError(result: Result): Unit = {
    when(
      mockErrorHandler.internalServerError()(any[AuthorisationRequest[_]]())
    ).thenReturn(result)
  }
}
