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

import connectors.CISConnector
import connectors.parsers.CISHttpParser.CISResponse
import models.submission.CISSubmission
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockCISConnector extends MockFactory { _: TestSuite =>

  protected val mockCISConnector: CISConnector = mock[CISConnector]

  def mockSubmit(nino: String,
                 taxYear: Int,
                 submission: CISSubmission,
                 result: CISResponse): CallHandler4[String, Int, CISSubmission, HeaderCarrier, Future[CISResponse]] = {
    (mockCISConnector.submit(_: String, _: Int, _: CISSubmission)(_: HeaderCarrier))
      .expects(nino, taxYear, submission, *)
      .returning(Future.successful(result))
  }

  def mockDelete(nino: String,
                 taxYear: Int,
                 submissionId: String,
                 result: CISResponse): CallHandler4[String, Int, String, HeaderCarrier, Future[CISResponse]] = {
    (mockCISConnector.delete(_: String, _: Int, _: String)(_: HeaderCarrier))
      .expects(nino, taxYear, submissionId, *)
      .returning(Future.successful(result))
  }
}
