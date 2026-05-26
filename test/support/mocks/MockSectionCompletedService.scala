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

import models.mongo.JourneyAnswers
import models.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import services.SectionCompletedService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSectionCompletedService { this: TestSuite =>

  protected val mockSectionCompletedService: SectionCompletedService =
    org.mockito.Mockito.mock(classOf[SectionCompletedService])

  def mockGet(mtdItId: String, taxYear: Int, journey: String, result: Option[JourneyAnswers]): Unit = {
    when(
      mockSectionCompletedService.get(
        eqTo(mtdItId),
        eqTo(taxYear),
        eqTo(journey)
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(result))
  }

  def mockSet(result: Done): Unit = {
    when(
      mockSectionCompletedService.set(any[JourneyAnswers]())(any[HeaderCarrier]())
    ).thenReturn(Future.successful(result))
  }

}
