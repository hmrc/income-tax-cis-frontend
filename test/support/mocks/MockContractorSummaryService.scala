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

import models.{IncomeTaxUserData, ServiceError, User}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import services.ContractorSummaryService

import scala.concurrent.Future

trait MockContractorSummaryService { this: TestSuite =>


  protected val mockContractorSummaryService: ContractorSummaryService =
    org.mockito.Mockito.mock(classOf[ContractorSummaryService])

  def mockSaveCYAForNewCisDeduction(taxYear: Int,
                                employerRef: String,
                                result: Either[ServiceError, Unit]): Unit = {
    when(
      mockContractorSummaryService.saveCYAForNewCisDeduction(
        any[Int](),
        any[String](),
        any[IncomeTaxUserData](),
        any[User]()
      )
    ).thenReturn(Future.successful(result))
  }
}
