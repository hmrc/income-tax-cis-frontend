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

package services

import models.{ServiceError, User}

import java.time.Month
import scala.concurrent.Future

class DeleteCISPeriodService {
  //TODO: - remove functionality to be implemented after Update/Delete CIS orchestration
  //TODO: - Update integration test to check if the item has been removed, when this is implemented.
  def removeCisDeduction(taxYear: Int, employerRef: String, user: User, deductionPeriod: Month): Future[Either[ServiceError, Unit]] = {
    Future.successful(Right(()))
  }
}
