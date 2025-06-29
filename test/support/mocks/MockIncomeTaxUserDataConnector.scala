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

import connectors.IncomeTaxUserDataConnector
import connectors.parsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.{APIErrorModel, IncomeTaxUserData}
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockIncomeTaxUserDataConnector extends MockFactory { _: TestSuite =>

  protected val mockIncomeTaxUserDataConnector: IncomeTaxUserDataConnector = mock[IncomeTaxUserDataConnector]

  def mockGetUserData(nino: String, taxYear: Int, result: Either[APIErrorModel, IncomeTaxUserData]): CallHandler3[String, Int, HeaderCarrier, Future[IncomeTaxUserDataResponse]] = {
    (mockIncomeTaxUserDataConnector.getUserData(_: String, _: Int)(_: HeaderCarrier))
      .expects(nino, taxYear, *)
      .returning(Future.successful(result))
  }
}
