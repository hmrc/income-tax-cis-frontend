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

import models._
import models.mongo.{CYAPeriodData, DataNotUpdatedError}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LabourPayService @Inject()(cisSessionService: CISSessionService)
                                (implicit ec: ExecutionContext) {

  def saveLabourPay(taxYear: Int,
                    employerRef: String,
                    user: User,
                    amount: BigDecimal): Future[Either[ServiceError, Unit]] = {

    cisSessionService.getSessionData(taxYear, employerRef, user).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(None) => Future.successful(Left(NoCisUserDataError))
      case Right(Some(cisUserData)) if !cisUserData.hasPeriodData => Future.successful(Left(NoCYAPeriodDataError))
      case Right(Some(cisUserData)) if cisUserData.hasPeriodData =>
        val periodData: CYAPeriodData = cisUserData.cis.periodData.map(_.copy(grossAmountPaid = Some(amount))).get
        val updatedCYA = cisUserData.cis.copy(periodData = Some(periodData))

        cisSessionService.createOrUpdateCISUserData(user, taxYear, employerRef, cisUserData.submissionId, cisUserData.isPriorSubmission, updatedCYA).map {
          case Left(_) => Left(DataNotUpdatedError)
          case Right(_) => Right(())
        }
    }
  }
}
