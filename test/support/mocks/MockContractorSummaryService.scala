/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import services.ContractorSummaryService

import scala.concurrent.Future

trait MockContractorSummaryService extends MockFactory {

  protected val mockContractorSummaryService: ContractorSummaryService = mock[ContractorSummaryService]

  def mockSaveCYAForNewCisDeduction(taxYear: Int,
                                employerRef: String,
                                result: Either[ServiceError, Unit]): CallHandler4[Int, String, IncomeTaxUserData, User, Future[Either[ServiceError, Unit]]] = {
    (mockContractorSummaryService.saveCYAForNewCisDeduction(_: Int, _: String, _: IncomeTaxUserData, _: User))
      .expects(taxYear, employerRef, *, *)
      .returns(Future.successful(result))
  }
}
