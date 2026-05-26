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

import models.mongo.CisUserData
import models.submission.CISSubmission
import models.{ServiceError, User}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import services.ContractorCYAService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockContractorCYAService { this: TestSuite =>


  protected val mockContractorCYAService: ContractorCYAService =
    org.mockito.Mockito.mock(classOf[ContractorCYAService])

  def mockSubmitCisDeductionCYA(taxYear: Int,
                                employerRef: String,
                                user: User,
                                cisUserData: CisUserData,
                                result: Either[ServiceError, Unit]): Unit = {
    when(
      mockContractorCYAService.submitCisDeductionCYA(
        eqTo(taxYear),
        eqTo(employerRef),
        eqTo(user),
        eqTo(cisUserData)
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(result))
  }

  def mockSubmitCisDeductionCYATailoring(taxYear: Int,
                                user: User,
                                result: Either[ServiceError, Unit]): Unit = {
    when(
      mockContractorCYAService.submitZeroCisDeductionTailor(
        eqTo(taxYear),
        any[String](),
        eqTo(user),
        any[CISSubmission]()
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(result))
  }
}
