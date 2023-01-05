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

import models.mongo.CisUserData
import models.{ServiceError, User}
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import services.DeductionPeriodService

import java.time.Month
import scala.concurrent.Future

trait MockDeductionPeriodService extends MockFactory {

  protected val mockDeductionPeriodService: DeductionPeriodService = mock[DeductionPeriodService]

  def mockSubmitMonth(taxYear: Int,
                       employerRef: String,
                       user: User,
                       month: Month,
                       result: Either[ServiceError, CisUserData]
                      ): CallHandler5[Int, String, User, Month, Option[String], Future[Either[ServiceError, CisUserData]]] = {
    (mockDeductionPeriodService.submitDeductionPeriod(_: Int, _: String, _: User, _: Month, _: Option[String]))
      .expects(taxYear, employerRef, user, month, *)
      .returns(Future.successful(result))
  }


}
