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

import connectors.RefreshIncomeSourceConnector
import connectors.parsers.RefreshIncomeSourceHttpParser.RefreshIncomeSourceResponse
import models.APIErrorModel
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockRefreshIncomeSourceConnector extends MockFactory { _: TestSuite =>

  protected val mockRefreshIncomeSourceConnector: RefreshIncomeSourceConnector = mock[RefreshIncomeSourceConnector]

  def mockRefresh(nino: String, taxYear: Int, result: Either[APIErrorModel, Unit]): CallHandler3[Int, String, HeaderCarrier, Future[RefreshIncomeSourceResponse]] = {
    (mockRefreshIncomeSourceConnector.put(_: Int,_: String)(_: HeaderCarrier))
      .expects(taxYear, nino, *)
      .returning(Future.successful(result))
  }
}
