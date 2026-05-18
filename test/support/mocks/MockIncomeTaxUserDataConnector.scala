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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockIncomeTaxUserDataConnector { this: TestSuite =>


  protected val mockIncomeTaxUserDataConnector: IncomeTaxUserDataConnector =
    org.mockito.Mockito.mock(classOf[IncomeTaxUserDataConnector])

  def mockGetUserData(nino: String, taxYear: Int, result: Either[APIErrorModel, IncomeTaxUserData]): Unit = {
    when(
      mockIncomeTaxUserDataConnector.getUserData(
        eqTo(nino),
        eqTo(taxYear)
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(result))
  }
}
