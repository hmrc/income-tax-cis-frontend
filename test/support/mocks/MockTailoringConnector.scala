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

import connectors.TailoringDataConnector
import connectors.parsers.ClearExcludedJourneysHttpParser.ClearExcludedJourneysResponse
import connectors.parsers.GetExcludedJourneysHttpParser.ExcludedJourneysResponse
import connectors.parsers.PostExcludedJourneyHttpParser.PostExcludedJourneyResponse
import models.tailoring.ExcludedJourneysResponseModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockTailoringConnector { this: TestSuite =>


  val mockTailoringDataConnector: TailoringDataConnector =
    org.mockito.Mockito.mock(classOf[TailoringDataConnector])

  def mockGetExcludedJourneys(userData: ExcludedJourneysResponseModel, taxYear: Int, nino: String): Unit = {
    when(
      mockTailoringDataConnector.getExcludedJourneys(
        eqTo(taxYear),
        eqTo(nino)
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(Right(userData)))
  }

  def mockClearExcludedJourneys(taxYear: Int, nino: String): Unit = {
    when(
      mockTailoringDataConnector.clearExcludedJourney(
        eqTo(taxYear),
        eqTo(nino)
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(Right(true)))
  }

  def mockPostExcludedJourneys(taxYear: Int, nino: String): Unit = {
    when(
      mockTailoringDataConnector.postExcludedJourney(
        eqTo(taxYear),
        eqTo(nino)
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(Right(true)))
  }

}
