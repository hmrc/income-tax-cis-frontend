/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalamock.handlers.CallHandler6
import org.scalamock.scalatest.MockFactory
import services.DeleteCISPeriodService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Month
import scala.concurrent.Future

trait MockDeleteCISPeriodService extends MockFactory {

  protected val mockService: DeleteCISPeriodService = mock[DeleteCISPeriodService]

  def mockRemoveCISDeduction(taxYear: Int,
                             employerRef: String,
                             user: User,
                             deductionPeriod: Month,
                             incomeTaxUserData: IncomeTaxUserData,
                             result: Either[ServiceError, Unit]): CallHandler6[Int, String, User, Month, IncomeTaxUserData, HeaderCarrier, Future[Either[ServiceError, Unit]]] = {
    (mockService.removeCisDeduction(_: Int, _: String, _: User, _: Month, _: IncomeTaxUserData)(_: HeaderCarrier))
      .expects(taxYear, employerRef, user, deductionPeriod, incomeTaxUserData, *)
      .returns(Future.successful(result))
  }
  def mockRemoveCISDeductionTailoring(taxYear: Int,
                             user: User,
                             incomeTaxUserData: IncomeTaxUserData,
                             result: Either[ServiceError, Unit]): CallHandler6[Int, String, User, Month, IncomeTaxUserData, HeaderCarrier, Future[Either[ServiceError, Unit]]] = {
    (mockService.removeCisDeduction(_: Int, _: String, _: User, _: Month, _: IncomeTaxUserData)(_: HeaderCarrier))
      .expects(taxYear, *, user, *, incomeTaxUserData, *)
      .returns(Future.successful(result)).anyNumberOfTimes()
  }
}
