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

import models.{ServiceError, User}
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import services.LabourPayService

import scala.concurrent.Future

trait MockLabourPayService extends MockFactory {

  protected val mockLabourPayService: LabourPayService = mock[LabourPayService]

  def mockSaveLabourPay(taxYear: Int,
                        employerRef: String,
                        user: User,
                        amount: BigDecimal,
                        result: Either[ServiceError, Unit]): CallHandler4[Int, String, User, BigDecimal, Future[Either[ServiceError, Unit]]] = {
    (mockLabourPayService.saveLabourPay(_: Int, _: String, _: User, _: BigDecimal))
      .expects(taxYear, employerRef, user, amount)
      .returning(Future.successful(result))
  }
}
