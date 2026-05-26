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

import connectors.parsers.GetExcludedJourneysHttpParser.ExcludedJourneysResponse
import models.tailoring.ExcludedJourneysResponseModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import services.TailoringService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockTailoringService { this: TestSuite =>


  val mockTailoringService: TailoringService =
    org.mockito.Mockito.mock(classOf[TailoringService])

  def mockGetExcludedJourneysFromService(userData: ExcludedJourneysResponseModel, taxYear: Int, nino: String): Unit = {
    when(
      mockTailoringService.getExcludedJourneys(
        eqTo(taxYear),
        eqTo(nino),
        any[String]()
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(Right(userData)))
  }

}
